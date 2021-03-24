/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/24/21, 10:39 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.io.MMU

class NoiseChannel : SoundChannel() {

    private val lfsr = Lfsr()
    private val polynomialCounter = PolynomialCounter()
    override val lengthCounter = LengthCounter(64, this)

    init {
        reset()
    }

    override fun reset() {
        super.reset()
        lengthCounter.setNr1(0xFF)
        polynomialCounter.reset()
        lfsr.reset()
    }

    override fun powerOff() {
        super.powerOff()
        reset()
    }

    override fun cycle(cycles: Int): Int {
        volumeEnvelope.cycle(cycles)
        lengthCounter.cycle(cycles)
        if (polynomialCounter.cycle(cycles)) {
            lastOutput = lfsr.cycle(cycles, polynomialCounter.width7)
        }
        return if (!enabled) 0 else lastOutput * volumeEnvelope.volume
    }

    override fun trigger() {
        polynomialCounter.trigger()
        lfsr.reset()
        super.trigger()
    }

    fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR42 -> volumeEnvelope.getNr2()
            MMU.NR43 -> polynomialCounter.getNr43()
            MMU.NR44 -> {
                var result = 0b10111111
                result = result.setBit(6, lengthCounter.lengthEnabled)
                result
            }
            else -> MMU.INVALID_READ
        }
    }

    fun writeByte(address: Int, value: Int) {
        when (address) {
            MMU.NR41 -> lengthCounter.setNr1(value and 0b00111111)
            MMU.NR42 -> {
                volumeEnvelope.setNr2(value)
                if (!volumeEnvelope.getDac()) {
                    enabled = false
                }
            }
            MMU.NR43 -> polynomialCounter.setNr43(value)
            MMU.NR44 -> {
                lengthCounter.setNr4(value)
                if (value.isBit(7)) trigger()
            }
        }
    }
}