/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 6:15 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import com.badlogic.gdx.utils.Disposable
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.isBit_
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.Interrupt
import xyz.angm.gamelin.system.io.GPUMode.*

// TODO: LCDC can be modified mid-scanline, account for that
class PPU(private val gb: GameBoy) : Disposable {

    val renderer = TileRenderer(gb, 20, 18, 4f)

    private var mode = OAMScan
    private var modeclock = 0
    private var line
        get() = gb.read(0xFF44)
        set(value) = gb.writeAny(0xFF44, value)
    private var lineCompare
        get() = gb.read(0xFF45)
        set(value) = gb.writeAny(0xFF45, value)
    private var stat
        get() = gb.read(0xFF41)
        set(value) = gb.writeAny(0xFF41, value)

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
    private val windowX get() = gb.read(0xFF4B)
    private val windowY get() = gb.read(0xFF4A)

    // All pixels in the current render cycle that have a non-null BG color (objects render under it)
    private val bgOccupiedPixels = Array(160 * 144) { false }

    fun step(tCycles: Int) {
        modeclock += tCycles
        if (modeclock < mode.cycles) return
        modeclock -= mode.cycles

        when (mode) {
            OAMScan -> mode = Upload

            Upload -> {
                mode = HBlank
                renderLine()
                statInterrupt(3)
            }

            HBlank -> {
                line++
                mode = if (line == 144) {
                    statInterrupt(4)
                    VBlank
                } else OAMScan
                statInterrupt(5)
                lycInterrupt()
            }

            VBlank -> {
                if (line == 144) gb.requestInterrupt(Interrupt.VBlank)
                line++
                if (line > 153) {
                    mode = OAMScan
                    line = 0
                    statInterrupt(5)
                    bgOccupiedPixels.fill(false)
                    renderer.finishFrame()
                }
                lycInterrupt()
            }
        }

        stat = (stat.toByte().setBit(2, if (lineCompare == line) 1 else 0) and 0b11111100) or mode.idx
    }

    private fun statInterrupt(index: Int) {
        if (stat.toByte().isBit_(index)) gb.requestInterrupt(Interrupt.LCDC)
    }

    private fun lycInterrupt() {
        if (lineCompare == line) statInterrupt(6)
    }

    private fun renderLine() {
        if (!displayEnable) return
        if (bgEnable) {
            renderBG()
            if (windowEnable) renderWindow()
        } else {
            clearLine()
        }
        if (objEnable) renderObjs()
    }

    private fun renderBG() {
        renderBGOrWindow(scrollX, bgMapAddr, bgMapLine) { if ((it and 0x1F) == 0x1F) it - 0x20 else it }
    }

    private fun renderWindow() {
        if (windowY > line) return
        renderBGOrWindow(windowX, windowMapAddr, line) { it }
    }

    private inline fun renderBGOrWindow(scrollX: Int, mapAddr: Int, mapLine: Int, tileAddrCorrect: (Int) -> Int) {
        var tileX = scrollX and 7
        val tileY = mapLine and 7
        var tileAddr = mapAddr + ((mapLine / 8) * 0x20) + (scrollX ushr 3)
        var tileDataAddr = bgTileDataAddr(gb.read(tileAddr)) + (tileY * 2)
        var high = gb.read(tileDataAddr + 1).toByte()
        var low = gb.read(tileDataAddr).toByte()

        for (tileIdxAddr in 0 until 160) {
            val colorIdx = (high.isBit(7 - tileX) shl 1) + low.isBit(7 - tileX)
            if (colorIdx != 0) setPixelOccupied(tileIdxAddr, line)
            renderer.drawPixel(tileIdxAddr, line, getBGColorIdx(colorIdx))

            if (++tileX == 8) {
                tileX = 0
                tileAddr = tileAddrCorrect(tileAddr)
                tileAddr++
                tileDataAddr = bgTileDataAddr(gb.read(tileAddr)) + (tileY * 2)
                high = gb.read(tileDataAddr + 1).toByte()
                low = gb.read(tileDataAddr).toByte()
            }
        }
    }

    private fun clearLine() {
        for (tileIdxAddr in 0 until 160) {
            renderer.drawPixel(tileIdxAddr, line, 0)
        }
    }

    private fun renderObjs() {
        var objCount = 0
        for (loc in 0xFE00 until 0xFEA0 step 4) {
            Sprite.dat = gb.read16(loc) + (gb.read16(loc + 2) shl 16)
            if (Sprite.y <= line && (Sprite.y + if (bigObjMode) 16 else 8) > line) { // If on this line
                renderObj()
                objCount++
                if (objCount == 10) break // At most 10 objects per scanline
            }
        }
    }

    private fun renderObj() = Sprite.run {
        val palette = if (altPalette) objPalette2 else objPalette1
        val tileYOp = (line - y) and 0x07
        val tileY = if (yFlip) 7 - tileYOp else tileYOp

        val tileNum = when {
            bigObjMode && (((line - y) <= 7) != yFlip) -> tilenum and 0xFE
            bigObjMode -> tilenum or 0x01
            else -> tilenum
        }

        val tileDataAddr = objTileOffset(tileNum) + (tileY * 2)
        val high = gb.read(tileDataAddr + 1).toByte()
        val low = gb.read(tileDataAddr).toByte()

        for (tileX in 0 until 8) {
            val colorIdx = if (!xFlip) (high.isBit(7 - tileX) shl 1) + low.isBit(7 - tileX) else (high.isBit(tileX) shl 1) + low.isBit(tileX)
            val screenX = x + tileX
            if ((screenX) >= 0 && (screenX) < 160 && colorIdx != 0 && (priority || !getPixelOccupied(screenX, line)))
                renderer.drawPixel(screenX, line, getColorIdx(palette, colorIdx))
        }
    }

    private fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = true
    }

    private fun getPixelOccupied(x: Int, y: Int) = bgOccupiedPixels[(x * 144) + y]

    fun bgIdxTileDataAddr(window: Boolean, idx: Int) = bgTileDataAddr(gb.read((if (window) windowMapAddr else bgMapAddr) + idx))

    private fun bgTileDataAddr(idx: Int): Int {
        return if (gb.read(0xFF40).isBit(4)) 0x8000 + (idx * 0x10)
        else 0x9000 + (idx.toByte() * 0x10)
    }

    private fun objTileOffset(idx: Int) = 0x8000 + (idx * 0x10)

    private fun getBGColorIdx(color: Int) = getColorIdx(bgPalette, color)

    private fun getColorIdx(palette: Int, color: Int) = (palette ushr (color * 2)) and 0b11

    fun reset() {
        mode = OAMScan
        modeclock = 0
        line = 0
    }

    override fun dispose() = renderer.dispose()
}

private enum class GPUMode(val cycles: Int, val idx: Int) {
    HBlank(204, 0), VBlank(456, 1), OAMScan(80, 2), Upload(172, 3)
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
