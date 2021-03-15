/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 4:12 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.GameBoy

class BGMapViewer(private val gb: GameBoy) : Window("BG Map Viewer") {

    private val renderer = TileRenderer(gb, 32, 32, 2f)

    init {
        add(renderer)
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        for (tileIdxAddr in 0 until 32 * 32) {
            val dataAddr = gb.ppu.bgIdxTileDataAddr(tileIdxAddr)
            renderer.drawTile(tileIdxAddr % 0x20, tileIdxAddr / 0x20, dataAddr) { it }
        }
    }
}