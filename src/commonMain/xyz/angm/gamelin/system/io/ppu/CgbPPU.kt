/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 11:00 PM.
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
import kotlin.jvm.Transient
import kotlin.math.min

/** The GameBoy Color's PPU. */
internal class CgbPPU(mmu: MMU, renderer: TileRenderer) : PPU(mmu, renderer) {

    private var bgPaletteIndex = 0
    private var bgPaletteIncrement = false
    private val bgPalettes = Array(4 * 8) { Color() } // 8 palettes with 4 colors each.
    private var objPaletteIndex = 0
    private var objPaletteIncrement = false
    private val objPalettes = Array(4 * 8) { Color() } // 8 palettes with 4 colors each.

    // All pixels where either:
    // - BG bit 7 is set - making all objs render below it
    // - An obj has already rendered, not allowing other objs to render
    @Transient private var unavailablePixels = Array(160 * 144) { false }

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
        val palette = palettes[(index ushr 1) and 0x1F]
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

    override fun vblankEnd() {
        super.vblankEnd()
        unavailablePixels.fill(false)
    }

    // See note on `setPixelOccupied` for details on this
    override fun renderLine() {
        if (!displayEnable) return
        renderBG()
        if (windowEnable) renderWindow()
        if (objEnable) renderObjs()
    }

    override fun renderBGOrWindow(scrollX: Int, startX: Int, endX: Int, mapAddr: Int, mapLine: Int, correctTileAddr: Boolean) {
        var tileX = scrollX and 7
        var tileAddr = mapAddr + ((mapLine / 8) * 0x20) + (scrollX ushr 3)
        var attributes = mmu.vram[0x2000 + (tileAddr and 0x1FFF)].int()
        var hasPrio = attributes.isBit(7) && bgEnable
        var tileY = if (attributes.isBit(6)) 7 - (mapLine and 7) else (mapLine and 7)
        var tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2) + attributes.bit(3) * 0x2000
        var high = mmu.vram[tileDataAddr + 1]
        var low = mmu.vram[tileDataAddr]

        for (tileIdxAddr in startX until endX) {
            val x = if (attributes.isBit(5)) tileX else 7 - tileX
            val colorIdx = (high.bit(x) shl 1) + low.bit(x)
            if (colorIdx != 0) {
                setPixelOccupied(tileIdxAddr, line)
                unavailablePixels[(tileIdxAddr * 144) + line] = hasPrio
            }

            val palette = attributes and 7
            val color = bgPalettes[(palette * 4) + colorIdx]
            renderer.drawPixel(tileIdxAddr, line, color.red, color.green, color.blue)

            if (++tileX == 8) {
                tileX = 0
                tileAddr = if (correctTileAddr && (tileAddr and 0x1F) == 0x1F) tileAddr - 0x20 else tileAddr
                tileAddr++
                attributes = mmu.vram[0x2000 + (tileAddr and 0x1FFF)].int()
                hasPrio = attributes.isBit(7) && bgEnable
                tileY = if (attributes.isBit(6)) 7 - (mapLine and 7) else (mapLine and 7)
                tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2) + attributes.bit(3) * 0x2000
                high = mmu.vram[tileDataAddr + 1]
                low = mmu.vram[tileDataAddr]
            }
        }
    }

    override fun getBGAddrAdjust(tileAddr: Int): Int {
        val attributes = mmu.vram[0x2000 + (tileAddr and 0x1FFF)].int()
        return attributes.bit(3) * 0x2000
    }

    override fun allowObj(objCount: Int) = true

    override fun drawObjPixel(x: Int, y: Int, colorIdx: Int, dmgPalette: Int) {
        unavailablePixels[(x * 144) + y] = colorIdx != 0
        val color = objPalettes[(Sprite.cgbPalette * 4) + colorIdx]
        renderer.drawPixel(x, y, color.red, color.green, color.blue)
    }

    override fun vramObjAddrOffset() = Sprite.cgbBank * 0x2000

    override fun isPixelFree(x: Int, y: Int, objPriority: Boolean): Boolean {
        // If the BG has priority at that location, the pixel is never free
        return super.isPixelFree(x, y, objPriority) && !unavailablePixels[(x * 144) + y]
    }

    /** LCDC.0 on CGB doesn't actually disable the BG and window,
     * it instead just makes them lose priority; therefore simply
     * don't set occupied pixels if LCDC.0 == 0. */
    override fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = bgEnable
    }

    override fun restore() {
        super.restore()
        unavailablePixels = Array(160 * 144) { false }
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