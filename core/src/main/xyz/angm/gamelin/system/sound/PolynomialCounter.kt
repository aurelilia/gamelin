/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 11:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

class PolynomialCounter {

    private var shiftedDivisor = 0
    private var i = 0

    fun setNr43(value: Int) {
        val clockShift = value shr 4
        val divisor = when (value and 7) {
            0 -> 8
            1 -> 16
            2 -> 32
            3 -> 48
            4 -> 64
            5 -> 80
            6 -> 96
            7 -> 112
            else -> throw IllegalStateException()
        }
        shiftedDivisor = divisor shl clockShift
        i = 1
    }

    fun tick(): Boolean {
        return if (--i == 0) {
            i = shiftedDivisor
            true
        } else {
            false
        }
    }
}
