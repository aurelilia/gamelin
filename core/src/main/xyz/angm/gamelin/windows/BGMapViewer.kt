/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 8:08 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisLabel
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.GameBoy

class BGMapViewer(private val gb: GameBoy) : Window("BG Map Viewer") {

    private val bgRender = TileRenderer(gb.mmu, 32, 32, 2f)
    private val windowRender = TileRenderer(gb.mmu, 32, 32, 2f)

    init {
        add(VisLabel("Background"))
        add(VisLabel("Window")).row()
        add(bgRender).padRight(3f)
        add(windowRender)
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        draw(bgRender, false)
        draw(windowRender, true)
    }

    private fun draw(renderer: TileRenderer, window: Boolean) {
        for (tileIdxAddr in 0 until 32 * 32) {
            val dataAddr = gb.mmu.ppu.bgIdxTileDataAddr(window, tileIdxAddr)
            renderer.drawTile(tileIdxAddr % 0x20, tileIdxAddr / 0x20, dataAddr) { it }
        }
        renderer.finishFrame()
    }
}