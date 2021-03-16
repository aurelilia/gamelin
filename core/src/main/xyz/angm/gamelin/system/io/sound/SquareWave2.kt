/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 6:53 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.system.io.MMU

class SquareWave2 : SquareWave() {

    override val lengthCounter = LengthCounter(64, this)
    private var frequency = 0

    init {
        reset()
    }

    override fun reset() {
        super.reset()
        duty = 0
        frequency = 0
    }

    override fun powerOff() {
        super.powerOff()
        frequency = 0
    }

    override fun writeByte(address: Int, value: Int) {
        val newVal = value and 0xFF

        when (address) {
            MMU.NR23 -> frequency = (frequency and 0b11100000000) or newVal
            MMU.NR24 -> {
                frequency = (frequency and 0b11111111) or ((newVal and 0b111) shl 8)
                super.writeByte(address, newVal)
            }
            else -> super.writeByte(address, newVal)
        }
    }

    override fun getFrequency(): Int {
        return 2048 - frequency
    }
}