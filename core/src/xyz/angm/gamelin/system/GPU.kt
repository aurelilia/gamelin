/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 9:27 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.GPUMode.*

class GPU(private val gb: GameBoy) {

    val renderer = TileRenderer(gb, 20, 18, 4f)

    private var mode = OAMScan
    private var modeclock = 0
    private var line
        get() = gb.read(0xFF44)
        set(value) = gb.writeAny(0xFF44, value)

    private val control get() = gb.read(0xFF40)
    private val displayEnable get() = control.isBit(7)
    private val bgEnable get() = control.isBit(0)
    private val spriteEnable get() = control.isBit(1)
    private val windowEnable get() = control.isBit(5)
    private val bigSpriteMode get() = control.isBit(2)

    private val scrollX get() = gb.read(0xFF43)
    private val scrollY get() = gb.read(0xFF42)
    private val bgMapLine get() = (scrollY + line) and 0xFF

    private val bgPalette get() = gb.readAny(0xFF47)
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
                renderLine()
            }

            mode == HBlank && modeclock >= 204 -> {
                modeclock = 0
                line++
                mode = if (line == 143) VBlank else OAMScan
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
        if (spriteEnable) renderSprites()
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
                tileAddr++
                tileDataAddr = bgTileDataAddr(gb.read(tileAddr)) + (tileY * 2)
                high = gb.read(tileDataAddr).toByte()
                low = gb.read(tileDataAddr + 1).toByte()
            }
        }
    }

    private fun renderWindow() {
        // TODO
    }

    private fun renderSprites() {
        // TODO
    }

    fun bgIdxTileDataAddr(idx: Int): Int {
        val tileIdx = gb.read(bgMapAddr + idx)
        // println("idx: $idx adddr: ${(bgMapAddr + idx).hex16()} tile: $tileIdx FF40: ${gb.read(0xFF40).hex16()}")
        return bgTileDataAddr(tileIdx)
    }

    private fun bgTileDataAddr(idx: Int): Int {
        return if (gb.read(0xFF40).isBit(4)) 0x8000 + (idx * 0x10)
        else 0x9000 + (idx.toByte() * 0x10)
    }

    private fun getBGColorIdx(color: Int) = (bgPalette ushr (color * 2)) and 0b11
}

private enum class GPUMode {
    HBlank, VBlank, OAMScan, Render
}
