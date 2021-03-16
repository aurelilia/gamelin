/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:52 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.system.io.MMU

class SquareWave1 : SquareWave() {

    private val frequencySweep = FrequencySweep(this)
    override val lengthCounter = LengthCounter(64, this)

    init {
        reset()
    }

    override fun powerOn() {
        super.powerOn()
        frequencySweep.powerOn()
    }

    override fun reset() {
        super.reset()
        duty = 0b10
        volumeEnvelope.setNr2(0xF3)
        enabled = true
        frequencySweep.reset()
    }

    override fun cycle(cycles: Int): Int {
        frequencySweep.cycle(cycles)
        return super.cycle(cycles)
    }

    override fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR10 -> frequencySweep.getNr10()
            else -> super.readByte(address)
        }
    }

    override fun writeByte(address: Int, value: Int) {
        when (address) {
            MMU.NR10 -> frequencySweep.setNr10(value)
            MMU.NR13 -> frequencySweep.setNr13(value)
            MMU.NR14 -> {
                frequencySweep.setNr14(value)
                super.writeByte(address, value)
            }
            else -> super.writeByte(address, value)
        }
    }

    override fun getFrequency() = frequencySweep.getFrequency()

    override fun trigger() {
        super.trigger()
        frequencySweep.trigger()
    }
}