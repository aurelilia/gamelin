/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 9:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisWindow
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.GameBoy

class VRAMViewer(gb: GameBoy) : VisWindow("VRAM Viewer") {

    private val renderer = TileRenderer(gb, 16, 24, 4f)

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
    }
}