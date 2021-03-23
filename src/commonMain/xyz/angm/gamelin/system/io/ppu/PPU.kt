/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/23/21, 6:21 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.*
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.cpu.Interrupt
import xyz.angm.gamelin.system.io.IODevice
import xyz.angm.gamelin.system.io.MMU
import xyz.angm.gamelin.system.io.ppu.GPUMode.*
import xyz.angm.gamelin.system.io.ppu.Sprite.dat
import kotlin.jvm.Transient

/** The Pixel Processing Unit of the system. This is an abstract class due to the different
 * PPUs of DMG and CGB. */
internal abstract class PPU(protected val mmu: MMU, @Transient var renderer: TileRenderer) : IODevice(), Disposable {

    private var mode = OAMScan
    private var modeclock = 0

    private var lcdc = 0
    protected var displayEnable = false
    protected var bgEnable = false
    protected var objEnable = false
    protected var windowEnable = false
    private var bigObjMode = false
    private var altBgTileData = false
    private var bgMapAddr = 0x1800
    private var windowMapAddr = 0x1800

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
            MMU.LY -> {
                // Braces are required here - https://youtrack.jetbrains.com/issue/KT-43374
                if (displayEnable) line else 0
            }
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
                bgMapAddr = if (!lcdc.isBit(3)) 0x1800 else 0x1C00
                altBgTileData = lcdc.isBit(4)
                windowEnable = lcdc.isBit(5)
                windowMapAddr = if (!lcdc.isBit(6)) 0x1800 else 0x1C00
                displayEnable = lcdc.isBit(7)

                if (!displayEnable) {
                    stat = stat and (0xF8)
                }
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
                    mmu.requestInterrupt(Interrupt.VBlank)
                    VBlank
                } else OAMScan
                statInterrupt(5)
                lycInterrupt()
            }

            VBlank -> {
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

        stat = (stat.toByte().setBit(2, if (lineCompare == line) 1 else 0) and 0b11111100) or mode.ordinal
    }

    private fun statInterrupt(index: Int) {
        if (stat.toByte().isBit(index)) mmu.requestInterrupt(Interrupt.STAT)
    }

    private fun lycInterrupt() {
        if (lineCompare == line) statInterrupt(6)
    }

    /** Called right before HBLANK starts, render the current line to the buffer. */
    protected abstract fun renderLine()

    protected fun renderBG() {
        renderBGOrWindow(scrollX, 0, bgMapAddr, (scrollY + line) and 0xFF) { if ((it and 0x1F) == 0x1F) it - 0x20 else it }
    }

    protected fun renderWindow() {
        if (windowY > line) return
        renderBGOrWindow(0, windowX, windowMapAddr, line) { it }
    }

    private fun renderBGOrWindow(scrollX: Int, startX: Int, mapAddr: Int, mapLine: Int, tileAddrCorrect: (Int) -> Int) {
        var tileX = scrollX and 7
        val tileY = mapLine and 7
        var tileAddr = mapAddr + ((mapLine / 8) * 0x20) + (scrollX ushr 3)
        var tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2) + getBGAddrAdjust(tileAddr)
        var high = mmu.vram[tileDataAddr + 1]
        var low = mmu.vram[tileDataAddr]

        for (tileIdxAddr in startX until 160) {
            val colorIdx = (high.bit(7 - tileX) shl 1) + low.bit(7 - tileX)
            if (colorIdx != 0) setPixelOccupied(tileIdxAddr, line)
            drawBGorWindowPixel(tileIdxAddr, line, colorIdx, tileAddr)

            if (++tileX == 8) {
                tileX = 0
                tileAddr = tileAddrCorrect(tileAddr)
                tileAddr++
                tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2) + getBGAddrAdjust(tileAddr)
                high = mmu.vram[tileDataAddr + 1]
                low = mmu.vram[tileDataAddr]
            }
        }
    }

    /** Returns the amount the BG map tile data pointer should be adjusted by, based
     * on the tile pointer. Used on CGB to implement bit 3 of the BG map attributes (tile bank selector) */
    protected abstract fun getBGAddrAdjust(tileAddr: Int): Int

    /** Draw a pixel of the BG or window to the buffer..
     * @param colorIdx The color of the palette to use
     * @param tileAddr The tile address of the tile this pixel is in, used for retrieving additional tile data on CGB */
    protected abstract fun drawBGorWindowPixel(x: Int, y: Int, colorIdx: Int, tileAddr: Int)

    protected fun clearLine() {
        for (tileIdxAddr in 0 until 160) {
            renderer.drawPixel(tileIdxAddr, line, dmgColors[0])
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
        val dmgPalette = if (dmgPalette) objPalette2 else objPalette1
        val tileYOp = (line - y) and 0x07
        val tileY = if (yFlip) 7 - tileYOp else tileYOp

        val tileNum = when {
            bigObjMode && (((line - y) <= 7) != yFlip) -> tilenum and 0xFE
            bigObjMode -> tilenum or 0x01
            else -> tilenum
        }

        val tileDataAddr = objTileOffset(tileNum) + (tileY * 2) + vramSpriteAddrOffset()
        val high = mmu.vram[tileDataAddr + 1]
        val low = mmu.vram[tileDataAddr]

        for (tileX in 0 until 8) {
            val colorIdx = if (!xFlip) (high.bit(7 - tileX) shl 1) + low.bit(7 - tileX) else (high.bit(tileX) shl 1) + low.bit(tileX)
            val screenX = x + tileX
            if ((screenX) >= 0 && (screenX) < 160 && colorIdx != 0 && (priority || !getPixelOccupied(screenX, line))) {
                drawObjPixel(screenX, line, colorIdx, dmgPalette)
            }
        }
    }

    /** Draw a pixel of the current sprite.
     * @param colorIdx The color of the palette to use
     * @param dmgPalette The selected DMG palette, not applicable to CGB color palettes */
    protected abstract fun drawObjPixel(x: Int, y: Int, colorIdx: Int, dmgPalette: Int)

    /** Should return the VRAM bank offset of the current sprite; always 0x0 on DMG. */
    protected abstract fun vramSpriteAddrOffset(): Int

    /** Set a pixel to be 'occupied' by the BG, preventing sprites without priority to draw above it. */
    protected abstract fun setPixelOccupied(x: Int, y: Int)

    private fun getPixelOccupied(x: Int, y: Int) = bgOccupiedPixels[(x * 144) + y]

    fun bgIdxTileDataAddr(window: Boolean, idx: Int): Int {
        val addr = (if (window) windowMapAddr else bgMapAddr) + idx
        return bgTileDataAddr(mmu.vram[addr]) + getBGAddrAdjust(addr)
    }


    private fun bgTileDataAddr(idx: Byte): Int {
        return if (altBgTileData) (idx.int() * 0x10)
        else 0x1000 + (idx * 0x10)
    }

    private fun objTileOffset(idx: Int) = (idx * 0x10)

    protected fun getBGColor(color: Int) = getColor(bgPalette, color)

    protected fun getColor(palette: Int, color: Int) = dmgColors[(palette ushr (color * 2)) and 0b11]

    protected fun TileRenderer.drawPixel(x: Int, y: Int, c: Int) = drawPixel(x, y, c, c, c)

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
        bgMapAddr = 0x1800
        windowMapAddr = 0x1800

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

    companion object {
        val dmgColors = intArrayOf(255, 191, 63, 0)
    }
}

private enum class GPUMode(val cycles: Int) {
    HBlank(204), VBlank(456), OAMScan(80), Upload(172)
}

/** Sprite data from OAM.
 * @property dat Integer containing the 4 bytes that make up this sprite's OAM data. */
internal object Sprite {
    var dat = 0
    val x get() = ((dat ushr 8) and 0xFF) - 8
    val y get() = (dat and 0xFF) - 16
    val tilenum get() = (dat ushr 16) and 0xFF

    private val options get() = (dat ushr 24) and 0xFF
    val dmgPalette get() = options.isBit(4)
    val xFlip get() = options.isBit(5)
    val yFlip get() = options.isBit(6)
    val priority get() = !options.isBit(7)
    val cgbBank get() = options.bit(3)
    val cgbPalette get() = options and 3
}
