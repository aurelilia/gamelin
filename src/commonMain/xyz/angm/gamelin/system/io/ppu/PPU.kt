/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 10:16 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.Disposable
import xyz.angm.gamelin.bit
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.cpu.Interrupt
import xyz.angm.gamelin.system.io.IODevice
import xyz.angm.gamelin.system.io.MMU
import xyz.angm.gamelin.system.io.ppu.GPUMode.*

internal abstract class PPU(private val mmu: MMU, val renderer: TileRenderer) : IODevice(), Disposable {

    private var mode = OAMScan
    private var modeclock = 0

    private var lcdc = 0
    protected var displayEnable = false
    protected var bgEnable = false
    protected var objEnable = false
    protected var windowEnable = false
    private var bigObjMode = false
    private var altBgTileData = false
    private var bgMapAddr = 0x9800
    private var windowMapAddr = 0x9800

    private var line = 0
    private var lineCompare = 0

    private var stat = 0
    private var scrollX = 0
    private var scrollY = 0
    private var windowX = 0
    private var windowY = 0

    private var bgPalette = 0b11100100
    private var objPalette1 = 0b11100100
    private var objPalette2 = 0b11100100

    // All pixels in the current render cycle that have a non-zero BG color (objects render under it)
    protected val bgOccupiedPixels = Array(160 * 144) { false }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.LCDC -> lcdc
            MMU.STAT -> stat
            MMU.SCY -> scrollY
            MMU.SCX -> scrollX
            MMU.LY -> line
            MMU.LYC -> lineCompare
            MMU.BGP -> bgPalette
            MMU.OBP0 -> objPalette1
            MMU.OBP1 -> objPalette2
            MMU.WX -> windowX + 7
            MMU.WY -> windowY
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.LCDC -> {
                lcdc = value
                bgEnable = lcdc.isBit(0)
                objEnable = lcdc.isBit(1)
                bigObjMode = lcdc.isBit(2)
                bgMapAddr = if (!lcdc.isBit(3)) 0x9800 else 0x9C00
                altBgTileData = lcdc.isBit(4)
                windowEnable = lcdc.isBit(5)
                windowMapAddr = if (!lcdc.isBit(6)) 0x9800 else 0x9C00
                displayEnable = lcdc.isBit(7)
            }
            MMU.STAT -> stat = value
            MMU.SCY -> scrollY = value
            MMU.SCX -> scrollX = value
            MMU.LYC -> lineCompare = value
            MMU.BGP -> bgPalette = value
            MMU.OBP0 -> objPalette1 = value
            MMU.OBP1 -> objPalette2 = value
            MMU.WX -> windowX = value - 7
            MMU.WY -> windowY = value
        }
    }

    fun step(tCycles: Int) {
        if (!displayEnable) return
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
                if (line == 144) mmu.requestInterrupt(Interrupt.VBlank)
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
        if (stat.toByte().isBit(index)) mmu.requestInterrupt(Interrupt.LCDC)
    }

    private fun lycInterrupt() {
        if (lineCompare == line) statInterrupt(6)
    }

    protected open fun renderLine() {
        if (!displayEnable) return
        if (bgEnable) {
            renderBG()
            if (windowEnable) renderWindow()
        } else {
            clearLine()
        }
        if (objEnable) renderObjs()
    }

    protected fun renderBG() {
        renderBGOrWindow(scrollX, 0, bgMapAddr, (scrollY + line) and 0xFF) { if ((it and 0x1F) == 0x1F) it - 0x20 else it }
    }

    protected fun renderWindow() {
        if (windowY > line) return
        renderBGOrWindow(0, windowX, windowMapAddr, line) { it }
    }

    private inline fun renderBGOrWindow(scrollX: Int, startX: Int, mapAddr: Int, mapLine: Int, tileAddrCorrect: (Int) -> Int) {
        var tileX = scrollX and 7
        val tileY = mapLine and 7
        var tileAddr = mapAddr + ((mapLine / 8) * 0x20) + (scrollX ushr 3)
        var tileDataAddr = bgTileDataAddr(mmu.read(tileAddr)) + (tileY * 2)
        var high = mmu.read(tileDataAddr + 1).toByte()
        var low = mmu.read(tileDataAddr).toByte()

        for (tileIdxAddr in startX until 160) {
            val colorIdx = (high.bit(7 - tileX) shl 1) + low.bit(7 - tileX)
            if (colorIdx != 0) setPixelOccupied(tileIdxAddr, line)
            renderer.drawPixel(tileIdxAddr, line, getBGColorIdx(colorIdx))

            if (++tileX == 8) {
                tileX = 0
                tileAddr = tileAddrCorrect(tileAddr)
                tileAddr++
                tileDataAddr = bgTileDataAddr(mmu.read(tileAddr)) + (tileY * 2)
                high = mmu.read(tileDataAddr + 1).toByte()
                low = mmu.read(tileDataAddr).toByte()
            }
        }
    }

    private fun clearLine() {
        for (tileIdxAddr in 0 until 160) {
            renderer.drawPixel(tileIdxAddr, line, 0)
        }
    }

    protected fun renderObjs() {
        var objCount = 0
        for (loc in 0xFE00 until 0xFEA0 step 4) {
            Sprite.dat = mmu.read16(loc) + (mmu.read16(loc + 2) shl 16)
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
        val high = mmu.read(tileDataAddr + 1).toByte()
        val low = mmu.read(tileDataAddr).toByte()

        for (tileX in 0 until 8) {
            val colorIdx = if (!xFlip) (high.bit(7 - tileX) shl 1) + low.bit(7 - tileX) else (high.bit(tileX) shl 1) + low.bit(tileX)
            val screenX = x + tileX
            if ((screenX) >= 0 && (screenX) < 160 && colorIdx != 0 && (priority || !getPixelOccupied(screenX, line)))
                renderer.drawPixel(screenX, line, getColorIdx(palette, colorIdx))
        }
    }

    protected open fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = true
    }

    private fun getPixelOccupied(x: Int, y: Int) = bgOccupiedPixels[(x * 144) + y]

    fun bgIdxTileDataAddr(window: Boolean, idx: Int) = bgTileDataAddr(mmu.read((if (window) windowMapAddr else bgMapAddr) + idx))

    private fun bgTileDataAddr(idx: Int): Int {
        return if (altBgTileData) 0x8000 + (idx * 0x10)
        else 0x9000 + (idx.toByte() * 0x10)
    }

    private fun objTileOffset(idx: Int) = 0x8000 + (idx * 0x10)

    private fun getBGColorIdx(color: Int) = getColorIdx(bgPalette, color)

    private fun getColorIdx(palette: Int, color: Int) = (palette ushr (color * 2)) and 0b11

    fun reset() {
        mode = OAMScan
        modeclock = 0

        lcdc = 0
        displayEnable = false
        bgEnable = false
        objEnable = false
        windowEnable = false
        bigObjMode = false
        altBgTileData = false
        bgMapAddr = 0x9800
        windowMapAddr = 0x9800

        line = 0
        lineCompare = 0

        stat = 0
        scrollX = 0
        scrollY = 0
        windowX = 0
        windowY = 0

        bgPalette = 0b11100100
        objPalette1 = 0b11100100
        objPalette2 = 0b11100100
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
