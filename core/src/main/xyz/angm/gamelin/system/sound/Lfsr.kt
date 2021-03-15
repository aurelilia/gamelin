/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 11:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

class Lfsr {

    private var lfsr = 0x7fff

    fun start() = reset()

    fun reset() {
        lfsr = 0x7fff
    }

    fun nextBit(widthMode7: Boolean): Int {
        val x = lfsr and 1 xor (lfsr and 2 shr 1) != 0
        lfsr = lfsr shr 1
        lfsr = lfsr or if (x) 1 shl 14 else 0
        if (widthMode7) {
            lfsr = lfsr or if (x) 1 shl 6 else 0
        }
        return 1 and lfsr.inv()
    }
}
