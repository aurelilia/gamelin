/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/31/21, 7:19 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.io.MMU

class WaveChannel : SoundChannel() {

    private var volumeShift = 4
    private var volumeCode = 0
    private var dac = false
    private val patternRam = IntArray(0x10)
    private var frequency = 0
    private var timer = 0
    private var positionCounter = 0
    override val lengthCounter = LengthCounter(256, this)

    override fun reset() {
        resetExceptRam()
        patternRam.fill(0)
    }

    override fun powerOff() {
        super.powerOff()
        resetExceptRam()
    }

    private fun resetExceptRam() {
        dac = false
        volumeShift = 4
        volumeCode = 0
        frequency = 0
        timer = 0
        positionCounter = 0
    }

    override fun cycle(cycles: Int): Int {
        lengthCounter.cycle(cycles)
        if (!enabled) return 0

        for (i in 0 until cycles) {
            if (--timer == 0) {
                // Reset timer
                timer = (2048 - frequency) * 2

                // Set sample buffer
                lastOutput = patternRam[positionCounter / 2]
                if (positionCounter % 2 == 0) {
                    lastOutput = lastOutput shr 4
                }
                lastOutput = (lastOutput and 0x0f) shr volumeShift

                // Increase sample position counter
                positionCounter = (positionCounter + 1) % 32
            }
        }

        return lastOutput
    }

    override fun trigger() {
        enabled = true
        timer = (2048 - frequency) * 2
        positionCounter = 0
        if (!dac) enabled = false
    }

    fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR30 -> {
                var result = 0b01111111
                result = result.setBit(7, dac)
                result
            }
            MMU.NR32 -> 0b10011111 or (volumeCode shl 5)
            MMU.NR34 -> {
                var result = 0b10111111
                result = result.setBit(6, lengthCounter.lengthEnabled)
                result
            }
            in MMU.WAVE_SAMPLES -> patternRam[address and 0x0F]
            else -> MMU.INVALID_READ
        }
    }

    fun writeByte(address: Int, value: Int) {
        when (address) {
            MMU.NR30 -> {
                dac = value.isBit(7)
                enabled = enabled && dac
            }
            MMU.NR31 -> lengthCounter.setNr1(value)
            MMU.NR32 -> {
                volumeCode = (value and 0b01100000) shr 5
                volumeShift = if (volumeCode == 0) 4 else volumeCode - 1
            }
            MMU.NR33 -> frequency = (frequency and 0b11100000000) or value
            MMU.NR34 -> {
                frequency = (frequency and 0b11111111) or ((value and 0b111) shl 8)
                lengthCounter.setNr4(value)
                if (value.isBit(7)) trigger()
            }
            in MMU.WAVE_SAMPLES -> patternRam[address and 0x0F] = value
        }
    }
}