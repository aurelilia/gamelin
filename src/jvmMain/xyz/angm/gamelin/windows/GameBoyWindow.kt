/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 5:34 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.gb

class GameBoyWindow : Window("GameBoy", closable = false) {

    private val renderCell = add(gb.mmu.ppu.renderer)

    init {
        pack()
        centerWindow()
    }

    fun refresh() {
        renderCell.setActor(gb.mmu.ppu.renderer)
        titleLabel.setText("GameBoy - ${gb.mmu.cart.getTitle()}")
        pack()
    }
}