/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 1:08 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy

class Debugger {

    var pcBreak = 0
    var pcBreakEnable = false
    var writeBreak = 0
    var writeBreakEnable = false
    var emuHalt = true

    fun process(gb: GameBoy) {
        if (pcBreakEnable && pcBreak == gb.cpu.pc.int()) {
            emuHalt = true
        }
    }
}