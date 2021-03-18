/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 8:07 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.system.GameBoy

class GameBoyWindow(gb: GameBoy) : Window("GameBoy", closable = false) {

    init {
        add(gb.mmu.ppu.renderer)
        pack()
        centerWindow()
    }

    fun updateTitle(gb: GameBoy) = titleLabel.setText("GameBoy - ${gb.mmu.cart.getTitle()}")
}