/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:42 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

class Lfsr {

    private var lfsr = 0x7fff

    fun reset() {
        lfsr = 0x7fff
    }

    fun cycle(cycles: Int, width7: Boolean): Int {
        for (i in 0 until cycles) {
            val x = lfsr and 1 xor (lfsr and 2 shr 1) != 0
            lfsr = lfsr shr 1
            lfsr = lfsr or if (x) 1 shl 14 else 0
            if (width7) {
                lfsr = lfsr or if (x) 1 shl 6 else 0
            }
        }
        return 1 and lfsr.inv()
    }
}