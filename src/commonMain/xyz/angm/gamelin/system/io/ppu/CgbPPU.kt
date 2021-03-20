/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 10:51 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.io.MMU

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
        val palette = palettes[index and 0x3E]
        return if (index.isBit(0)) ((rgbToGB(palette.green) ushr 3) and 3) or (rgbToGB(palette.blue) shl 2)
        else rgbToGB(palette.red) or ((rgbToGB(palette.green) shl 5) and 7)
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
        val palette = palettes[index and 0x1F]
        if (index.isBit(0)) {
            palette.green = gbToRGB((rgbToGB(palette.green) and 7) or ((value and 3) shl 3))
            palette.blue = gbToRGB((value ushr 2) and 0x1F)
        } else {
            palette.red = gbToRGB(value and 0x1F)
            palette.green = gbToRGB((rgbToGB(palette.green) and 0b11000) or (value ushr 5))
        }
    }

    // See note on `setPixelOccupied` for details on this
    override fun renderLine() {
        if (!displayEnable) return
        renderBG()
        if (windowEnable) renderWindow()
        if (objEnable) renderObjs()
    }

    // LCDC.0 on CGB doesn't actually disable the BG and window,
    // it instead just makes them lose priority; therefore simply
    // don't set occupied pixels if LCDC.0 == 0
    override fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = bgEnable
    }

    // Convert a 5-bit GBC color to 8-bit RGB
    private fun gbToRGB(gb: Int) = (gb shl 3) or (gb ushr 2)
    // Convert an 8-bit RGB color to a 5-bit GBC color
    private fun rgbToGB(rgb: Int) = rgb ushr 3
}

// Color in RGG888 format; each color in 0-255 range
internal data class Color(var red: Int = 255, var green: Int = 255, var blue: Int = 255)