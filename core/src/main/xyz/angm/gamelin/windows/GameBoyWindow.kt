/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/17/21, 7:29 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import xyz.angm.gamelin.system.GameBoy

class GameBoyWindow(gb: GameBoy) : Window("GameBoy", closable = false) {

    init {
        add(gb.ppu.renderer)
        pack()
        centerWindow()
    }

    fun updateTitle(gb: GameBoy) {
        val str = StringBuilder()
        for (byte in 0x134..0x013E) { // Title in Game ROM
            val value = gb.read(byte)
            if (value == 0x00) break
            str.append(value.toChar())
        }
        titleLabel.setText("GameBoy - $str")
    }
}