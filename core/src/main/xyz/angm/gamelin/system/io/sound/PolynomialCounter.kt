/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:50 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit

class PolynomialCounter {

    private var clockShift = 0
    private var divisorCode = 0
    private var shiftedDivisor = 0
    private var counter = 0
    var width7 = false

    init {
        reset()
    }

    fun reset() {
        shiftedDivisor = 0
        counter = 0
        clockShift = 0
        divisorCode = 0
        width7 = false
    }

    fun getNr43(): Int {
        var result = clockShift shl 4
        result = result.setBit(3, width7)
        result = result or divisorCode
        return result
    }

    fun setNr43(value: Int) {
        width7 = value.isBit(3)
        clockShift = value shr 4

        divisorCode = value and 0b111
        val divisor = if (divisorCode == 0) 8 else divisorCode shl 4
        shiftedDivisor = divisor shl clockShift
    }

    fun cycle(cycles: Int): Boolean {
        for (i in 0 until cycles) {
            counter--
            if (counter == 0) counter = shiftedDivisor * 4
        }
        return counter == shiftedDivisor * 4
    }

    fun trigger() {
        counter = shiftedDivisor * 4
    }
}