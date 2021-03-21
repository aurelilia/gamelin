/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:37 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.gb
import xyz.angm.gamelin.interfaces.TileRenderer

/** Shows the current VRAM contents at 0x8000-0x9800.
 * Does not consider CGB banks, shows currently selected bank. */
class VRAMViewer : Window("VRAM Viewer") {

    private val renderer = TileRenderer(gb.mmu, 16, 24, 4f)

    init {
        add(renderer)
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        for (tile in 0x8000 until 0x9800 step 0x10) {
            val tileIdx = (tile - 0x8000) / 0x10
            renderer.drawTile(tileIdx % 0x10, tileIdx / 0x10, tile) { it }
        }
        renderer.finishFrame()
    }
}