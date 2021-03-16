/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:51 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.isBit_
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.io.MMU

abstract class SquareWave : SoundChannel() {

    protected var duty = 0 // Current duty
    private var dutyCounter = 0 // Current bit in duty
    private var off = false
    protected var timer = 0

    override fun reset() {
        super.reset()
        dutyCounter = 0
        timer = 0
        off = false
    }

    override fun powerOn() {
        super.powerOn()
        off = false
    }

    override fun powerOff() {
        reset()
        off = true
        super.powerOff()
        duty = 0
    }

    override fun trigger() {
        super.trigger()
        timer = getFrequency() * 4
    }

    override fun cycle(cycles: Int): Int {
        volumeEnvelope.cycle(cycles)
        lengthCounter.cycle(cycles)

        timer -= cycles
        while (timer < 0) {
            timer += getFrequency() * 4
            lastOutput = dutyCycles[duty].isBit_(dutyCounter)
            dutyCounter = (dutyCounter + 1) and 7
        }

        return if (!enabled) 0 else lastOutput * volumeEnvelope.volume
    }

    open fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR11,
            MMU.NR21 -> (this.duty shl 6) or 0b00111111 // Only bits 6-7 can be read
            MMU.NR12,
            MMU.NR22 -> volumeEnvelope.getNr2()
            MMU.NR13,
            MMU.NR23 -> 0xFF
            MMU.NR14,
            MMU.NR24 -> {
                var result = 0b10111111
                result = result.setBit(6, lengthCounter.lengthEnabled)
                result
            }
            else -> MMU.INVALID_READ
        }
    }

    open fun writeByte(address: Int, value: Int) {
        val newVal = value and 0xFF
        when (address) {
            MMU.NR11,
            MMU.NR21 -> {
                // Only write to the length counter if the APU is off
                if (!off)
                    duty = (newVal shr 6) and 0b11

                // On DMG the length counters are not affected by power and can still be written while off
                lengthCounter.setNr1(newVal and 0b00111111)
            }
            MMU.NR12,
            MMU.NR22 -> {
                volumeEnvelope.setNr2(newVal)
                if (!volumeEnvelope.getDac()) {
                    enabled = false
                }
            }
            MMU.NR14,
            MMU.NR24 -> {
                lengthCounter.setNr4(newVal)
                if (newVal.isBit(7)) trigger()
            }
        }
    }

    abstract fun getFrequency(): Int

    companion object {
        private val dutyCycles = arrayOf(0b00000001, 0b10000001, 0b10000111, 0b01111110)
    }
}