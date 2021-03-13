/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 4:59 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.render.TileRenderer
import xyz.angm.gamelin.system.GPUMode.*

class GPU(private val gb: GameBoy) {

    val renderer = TileRenderer(gb, 20, 18, 4f)

    private var mode = OAMScan
    private var modeclock = 0
    private var line = 0

    private val scrollX get() = gb.read(0xFF43)
    private val scrollY get() = gb.read(0xFF42)

    private val bgPalette get() = gb.read(0xFF47)
    private val bgMapAddr get() = if (gb.read(0xFF40).isBit(3) == 0) 0x9800 else 0x9C00
    private val windowMapAddr get() = if (gb.read(0xFF40).isBit(6) == 0) 0x9800 else 0x9C00

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
        // TODO: Line renderer...
        for (tile in 0x8000 until 0x9000 step 0x10) {
            val tileIdx = (tile - 0x8000) / 0x10
            renderer.drawTile(tileIdx % 0x10, tileIdx / 0x10, tile) { it }
        }
    }

    fun bgIdxTileDataAddr(idx: Int): Int {
        val tileIdx = gb.read(bgMapAddr + idx)
        // print("idx: $idx adddr: ${(bgMapAddr + idx).hex16()} tile: $tileIdx FF40: ${gb.read(0xFF40).hex16()} ADDR: ")
        return bgTileDataAddr(tileIdx)
    }

    private fun bgTileDataAddr(idx: Int): Int {
        return if (gb.read(0xFF40).isBit(4) == 1) 0x8000 + (idx * 0x10)
        else 0x9000 + (idx.toByte() * 0x10)
    }
}

private enum class GPUMode {
    HBlank, VBlank, OAMScan, Render
}
