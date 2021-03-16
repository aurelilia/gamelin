/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 10:47 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.MMU

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

    override fun tick(): Int {
        frequencySweep.tick()
        return super.tick()
    }

    override fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR10 -> frequencySweep.getNr10()
            else -> super.readByte(address)
        }
    }

    override fun writeByte(address: Int, value: Int) {
        val newVal = value and 0xFF

        when (address) {
            MMU.NR10 -> frequencySweep.setNr10(newVal)
            MMU.NR13 -> frequencySweep.setNr13(newVal)
            MMU.NR14 -> {
                frequencySweep.setNr14(newVal)
                super.writeByte(address, newVal)
            }
            else -> super.writeByte(address, newVal)
        }
    }

    override fun getFrequency(): Int {
        return frequencySweep.getFrequency()
    }

    override fun trigger() {
        super.trigger()
        frequencySweep.trigger()
    }
}