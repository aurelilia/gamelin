/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:15 PM.
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
        when (address) {
            MMU.NR23 -> frequency = (frequency and 0b11100000000) or value
            MMU.NR24 -> {
                frequency = (frequency and 0b11111111) or ((value and 0b111) shl 8)
                super.writeByte(address, value)
            }
            else -> super.writeByte(address, value)
        }
    }

    override fun getFrequency() = 2048 - frequency
}