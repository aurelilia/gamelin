/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/22/21, 10:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.bit
import xyz.angm.gamelin.configuration
import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.io.MMU
import kotlin.math.min

/** The GameBoy Color's PPU. */
internal class CgbPPU(mmu: MMU, renderer: TileRenderer) : PPU(mmu, renderer) {

    private var bgPaletteIndex = 0
    private var bgPaletteIncrement = false
    private val bgPalettes = Array(4 * 8) { Color() } // 8 palettes with 4 colors each.
    private var objPaletteIndex = 0
    private var objPaletteIncrement = false
    private val objPalettes = Array(4 * 8) { Color() } // 8 palettes with 4 colors each.

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.BCPS -> readCPS(bgPaletteIndex, bgPaletteIncrement)
            MMU.BCPD -> readCPD(bgPaletteIndex, bgPalettes)
            MMU.OCPS -> readCPS(objPaletteIndex, objPaletteIncrement)
            MMU.OCPD -> readCPD(objPaletteIndex, objPalettes)
            else -> super.read(addr)
        }
    }

    private fun readCPS(index: Int, increment: Boolean) = index and (increment.int() shl 7)

    private fun readCPD(index: Int, palettes: Array<Color>): Int {
        val palette = palettes[index and 0x1E]
        return if (index.isBit(0)) palette.rawHigh else palette.rawLow
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.BCPS -> {
                bgPaletteIndex = value and 0x3F
                bgPaletteIncrement = value.isBit(7)
            }
            MMU.BCPD -> {
                writeCPD(bgPaletteIndex, bgPalettes, value)
                if (bgPaletteIncrement) bgPaletteIndex++
            }
            MMU.OCPS -> {
                objPaletteIndex = value and 0x3F
                objPaletteIncrement = value.isBit(7)
            }
            MMU.OCPD -> {
                writeCPD(objPaletteIndex, objPalettes, value)
                if (objPaletteIncrement) objPaletteIndex++
            }
            else -> super.write(addr, value)
        }
    }

    private fun writeCPD(index: Int, palettes: Array<Color>, value: Int) {
        val palette = palettes[(index ushr 1) and 0x1F]
        if (index.isBit(0)) palette.rawHigh = value
        else palette.rawLow = value
        palette.recalculate()
    }

    // See note on `setPixelOccupied` for details on this
    override fun renderLine() {
        if (!displayEnable) return
        renderBG()
        if (windowEnable) renderWindow()
        if (objEnable) renderObjs()
    }

    override fun getBGAddrAdjust(tileAddr: Int): Int {
        val attributes = mmu.vram[0x2000 + (tileAddr and 0x1FFF)].int()
        return attributes.bit(3) * 0x2000
    }

    override fun drawBGorWindowPixel(x: Int, y: Int, colorIdx: Int, tileAddr: Int) {
        if (tileAddr < 0x9C00) {
            val attributes = mmu.vram[0x2000 + (tileAddr and 0x1FFF)].int()
            val palette = attributes and 7
            val color = bgPalettes[(palette * 4) + colorIdx]
            renderer.drawPixel(x, y, color.red, color.green, color.blue)
        } else {
            // Does not have additional palette data
            renderer.drawPixel(x, y, colorIdx)
        }
    }

    override fun drawObjPixel(x: Int, y: Int, colorIdx: Int, dmgPalette: Int) {
        val color = objPalettes[(Sprite.cgbPalette * 4) + colorIdx]
        renderer.drawPixel(x, y, color.red, color.green, color.blue)
    }

    override fun vramSpriteAddrOffset() = Sprite.cgbBank * 0x2000

    /** LCDC.0 on CGB doesn't actually disable the BG and window,
     * it instead just makes them lose priority; therefore simply
     * don't set occupied pixels if LCDC.0 == 0. */
    override fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = bgEnable
    }
}

/** Color in RGG888 format; each color in 0-255 range.
 * Also contains the 2 raw color registers. */
data class Color(
    var red: Int = 255, var green: Int = 255, var blue: Int = 255,
    var rawLow: Int = 0xFF, var rawHigh: Int = 0x7F
) {

    /** Recalculate the RGB colors based off of raw registers. */
    fun recalculate() {
        setToGBColors()
        if (configuration.cgbColorCorrection) {
            // https://near.sh/articles/video/color-emulation
            red = (red * 26 + green * 4 + blue * 2)
            green = (green * 24 + blue * 8)
            blue = (red * 6 + green * 4 + blue * 22)
            red = min(960, red) ushr 2
            green = min(960, green) ushr 2
            blue = min(960, blue) ushr 2
        } else {
            red = toRGBdirect(red)
            green = toRGBdirect(green)
            blue = toRGBdirect(blue)
        }
    }

    /** Temporarily set r, g, b to 0-31 GB colors */
    private fun setToGBColors() {
        red = rawLow and 0x1F
        green = ((rawHigh and 3) shl 3) or (rawLow ushr 5)
        blue = (rawHigh ushr 2) and 0x1F
    }

    companion object {
        /** Simple linear mapping of 0-31 to 0-255. */
        private fun toRGBdirect(gb: Int) = (gb shl 3) or (gb ushr 2)
    }
}