/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 5:12 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.config
import xyz.angm.gamelin.gb

/** Window showing the GameBoy screen. */
class GameBoyWindow : Window("GameBoy", closable = false) {

    private val renderCell = add(gb.mmu.ppu.renderer)

    init {
        gb.mmu.ppu.renderer.setGBScale(config.gbScale.toFloat())
        pack()
        centerWindow()
        fadeIn()
    }

    fun refresh() {
        renderCell.setActor(gb.mmu.ppu.renderer)
        titleLabel.setText("GameBoy - ${gb.mmu.cart.getTitle()}")
        pack()
    }
}