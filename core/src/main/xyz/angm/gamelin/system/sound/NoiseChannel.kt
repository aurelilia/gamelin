/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:01 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.MMU

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

    override fun tick(): Int {
        volumeEnvelope.tick()

        lengthCounter.tick()

        if (polynomialCounter.tick()) {
            lastOutput = lfsr.nextBit(polynomialCounter.width7)
        }

        if (!enabled) {
            return 0
        }

        val volume = volumeEnvelope.volume
        return lastOutput * volume
    }

    override fun trigger() {
        polynomialCounter.trigger()
        lfsr.reset()
        super.trigger()
    }

    fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR41 -> 0xFF
            MMU.NR42 -> volumeEnvelope.getNr2()
            MMU.NR43 -> polynomialCounter.getNr43()
            MMU.NR44 -> {
                var result = 0b10111111
                result = result.setBit(6, lengthCounter.lengthEnabled)
                result
            }
            else -> throw IllegalArgumentException("Address ${address.hex16()} does not belong to NoiseChannel")
        }
    }

    fun writeByte(address: Int, value: Int) {
        val newVal = value and 0xFF
        when (address) {
            MMU.NR41 -> {
                lengthCounter.setNr1(newVal and 0b00111111)
            }
            MMU.NR42 -> {
                volumeEnvelope.setNr2(newVal)

                if (!volumeEnvelope.getDac()) {
                    enabled = false
                }
            }
            MMU.NR43 -> {
                polynomialCounter.setNr43(newVal)
            }
            MMU.NR44 -> {
                lengthCounter.setNr4(newVal)

                if (newVal.isBit(7)) {
                    trigger()
                }
            }
            else -> throw IllegalArgumentException("Address ${address.hex16()} does not belong to NoiseChannel")
        }
    }
}