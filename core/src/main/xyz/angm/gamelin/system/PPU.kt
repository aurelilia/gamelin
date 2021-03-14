/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 9:07 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import com.badlogic.gdx.utils.Disposable
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.GPUMode.*

class PPU(private val gb: GameBoy) : Disposable {

    val renderer = TileRenderer(gb, 20, 18, 4f)

    private var mode = OAMScan
    private var modeclock = 0
    private var line
        get() = gb.read(0xFF44)
        set(value) = gb.writeAny(0xFF44, value)

    private val control get() = gb.read(0xFF40)
    private val displayEnable get() = control.isBit(7)
    private val bgEnable get() = control.isBit(0)
    private val objEnable get() = control.isBit(1)
    private val windowEnable get() = control.isBit(5)
    private val bigObjMode get() = control.isBit(2)

    private val scrollX get() = gb.read(0xFF43)
    private val scrollY get() = gb.read(0xFF42)
    private val bgMapLine get() = (scrollY + line) and 0xFF

    private val bgPalette get() = gb.read(0xFF47)
    private val objPalette1 get() = gb.read(0xFF48)
    private val objPalette2 get() = gb.read(0xFF49)

    private val bgMapAddr get() = if (!control.isBit(3)) 0x9800 else 0x9C00
    private val windowMapAddr get() = if (!control.isBit(6)) 0x9800 else 0x9C00

    fun step(tCycles: Int) {
        modeclock += tCycles
        when {
            mode == OAMScan && modeclock >= 80 -> {
                modeclock = 0
                mode = Render
            }

            mode == Render && modeclock >= 172 -> {
                modeclock = 0
                mode = HBlank
                gb.requestInterrupt(Interrupt.HBlank)
                renderLine()
            }

            mode == HBlank && modeclock >= 204 -> {
                modeclock = 0
                line++
                mode = if (line == 143) {
                    gb.requestInterrupt(Interrupt.VBlank)
                    VBlank
                } else OAMScan
            }

            mode == VBlank && modeclock >= 456 -> {
                modeclock = 0
                line++
                if (line > 153) {
                    mode = OAMScan
                    line = 0
                }
            }
        }
    }

    private fun renderLine() {
        if (!displayEnable) return
        if (bgEnable) renderBG()
        if (windowEnable) renderWindow()
        if (objEnable) renderObjs()
    }

    private fun renderBG() {
        var tileX = scrollX and 7
        val tileY = bgMapLine and 7
        var tileAddr = bgMapAddr + ((bgMapLine / 8) * 0x20) + (scrollX ushr 3)
        var tileDataAddr = bgTileDataAddr(gb.read(tileAddr)) + (tileY * 2)
        var high = gb.read(tileDataAddr).toByte()
        var low = gb.read(tileDataAddr + 1).toByte()

        for (tileIdxAddr in 0 until 160) {
            val colorIdx = (high.isBit(7 - tileX) shl 1) + low.isBit(7 - tileX)
            renderer.drawPixel(tileIdxAddr, line, getBGColorIdx(colorIdx))

            if (++tileX == 8) {
                tileX = 0
                if ((tileAddr and 0x19) == 0x19) tileAddr -= 0x20
                tileAddr++
                tileDataAddr = bgTileDataAddr(gb.read(tileAddr)) + (tileY * 2)
                high = gb.read(tileDataAddr).toByte()
                low = gb.read(tileDataAddr + 1).toByte()
            }
        }
    }

    // TODO: 8x16 sprite mode
    private fun renderObjs() {
        for (loc in 0xFE00 until 0xFEA0 step 4) {
            Sprite.dat = gb.read16(loc) + (gb.read16(loc + 2) shl 16)
            renderObj()
        }
    }

    private fun renderObj() = Sprite.run {
        if (!(y <= line && (y + 8) > line)) return // Not on this line
        val palette = if (altPalette) objPalette1 else objPalette2
        val tileY = if (yFlip) 7 - (line - y) else line - y

        val tileDataAddr = objTileOffset(tilenum) + (tileY * 2)
        val high = gb.read(tileDataAddr).toByte()
        val low = gb.read(tileDataAddr + 1).toByte()

        for (tileX in 0 until 8) {
            val colorIdx = if (xFlip) (high.isBit(7 - tileX) shl 1) + low.isBit(7 - tileX) else (high.isBit(tileX) shl 1) + low.isBit(tileX)
            val screenX = x + tileX
            if ((screenX) >= 0 && (screenX) < 160 && colorIdx != 0 && (priority || renderer.isClear(screenX, line)))
                renderer.drawPixel(screenX, line, getColorIdx(palette, colorIdx))
        }
    }

    private fun renderWindow() {
        // TODO
    }

    fun bgIdxTileDataAddr(idx: Int): Int {
        val tileIdx = gb.read(bgMapAddr + idx)
        return bgTileDataAddr(tileIdx)
    }

    private fun bgTileDataAddr(idx: Int): Int {
        return if (gb.read(0xFF40).isBit(4)) 0x8000 + (idx * 0x10)
        else 0x9000 + (idx.toByte() * 0x10)
    }

    private fun objTileOffset(idx: Int) = 0x8000 + (idx * 0x10)

    private fun getBGColorIdx(color: Int) = getColorIdx(bgPalette, color)

    private fun getColorIdx(palette: Int, color: Int) = (palette ushr (color * 2)) and 0b11

    override fun dispose() = renderer.dispose()
}

private enum class GPUMode {
    HBlank, VBlank, OAMScan, Render
}

private object Sprite {
    var dat = 0
    val x get() = ((dat ushr 8) and 0xFF) - 8
    val y get() = (dat and 0xFF) - 16
    val tilenum get() = (dat ushr 16) and 0xFF
    val options get() = (dat ushr 24) and 0xFF
    val altPalette get() = options.isBit(4)
    val xFlip get() = options.isBit(5)
    val yFlip get() = options.isBit(6)
    val priority get() = !options.isBit(7)
}
