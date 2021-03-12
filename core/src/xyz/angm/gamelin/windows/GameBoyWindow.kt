/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 9:44 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisWindow
import xyz.angm.gamelin.system.GameBoy

class GameBoyWindow(gb: GameBoy) : VisWindow("GameBoy") {
    init {
        add(gb.gpu)
    }
}