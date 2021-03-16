/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:01 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.interfaces.AudioOutput
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.MMU

class Sound {

    private val square1 = SquareWave1()
    private val square2 = SquareWave2()
    private val wave = WaveChannel()
    private val noise = NoiseChannel()

    var optionChannelEnables = Array(4) { true }

    private val allChannels: Array<SoundChannel> = arrayOf(square1, square2, wave, noise)

    private var enabled = true

    var output = AudioOutput()
    private val samples = IntArray(4)

    private var vinLeft = false
    private var vinRight = false
    private var volumeLeft = 0
    private var volumeRight = 0
    private var leftEnables = Array(4) { false }
    private var rightEnables = Array(4) { false }

    init {
        reset()
    }

    fun reset() {
        vinLeft = false
        vinRight = false
        volumeLeft = 0b111
        volumeRight = 0b111

        leftEnables.fill(true)
        rightEnables[0] = true
        rightEnables[1] = true
        rightEnables[2] = false
        rightEnables[3] = false

        for (channel in allChannels) {
            channel.reset()
        }

        enabled = true
        samples.fill(0)
        output.reset()
    }

    fun step(tCycles: Int) {
        for (i in 0 until tCycles) tick()
    }

    fun tick() {
        for (i in 0 until 4) {
            samples[i] = allChannels[i].tick()
            if (!optionChannelEnables[i])
                samples[i] = 0
        }

        var left = 0
        var right = 0

        for (i in 0 until 4) {
            if (leftEnables[i]) {
                left += samples[i]
            }
            if (rightEnables[i]) {
                right += samples[i]
            }
        }

        left *= volumeLeft + 1
        right *= volumeRight + 1

        left /= 4
        right /= 4

        output.play(left.toByte(), right.toByte())
    }

    fun readByte(address: Int): Int {
        return when (address) {
            MMU.NR10,
            MMU.NR11,
            MMU.NR12,
            MMU.NR13,
            MMU.NR14 -> {
                square1.readByte(address)
            }
            MMU.NR21,
            MMU.NR22,
            MMU.NR23,
            MMU.NR24 -> {
                square2.readByte(address)
            }
            MMU.NR30,
            MMU.NR31,
            MMU.NR32,
            MMU.NR33,
            MMU.NR34,
            in 0xFF30..0xFF3F -> {
                wave.readByte(address)
            }
            MMU.NR41,
            MMU.NR42,
            MMU.NR43,
            MMU.NR44 -> {
                noise.readByte(address)
            }
            MMU.NR50 -> {
                var result = 0
                result = result.setBit(7, this.vinLeft)
                result = result or (this.volumeLeft shl 4)
                result = result.setBit(3, this.vinRight)
                result = result or (this.volumeRight)
                result
            }
            MMU.NR51 -> {
                var result = 0
                result = result.setBit(7, leftEnables[3])
                result = result.setBit(6, leftEnables[2])
                result = result.setBit(5, leftEnables[1])
                result = result.setBit(4, leftEnables[0])
                result = result.setBit(3, rightEnables[3])
                result = result.setBit(2, rightEnables[2])
                result = result.setBit(1, rightEnables[1])
                result = result.setBit(0, rightEnables[0])
                result
            }
            MMU.NR52 -> {
                // Bits 0-3 are statuses of channels (1, 2, wave, noise)
                var result = 0b01110000 // Bits 4-6 are unused
                if (square1.enabled) {
                    result = result.setBit(0)
                }
                if (square2.enabled) {
                    result = result.setBit(1)
                }
                if (wave.enabled) {
                    result = result.setBit(2)
                }
                if (noise.enabled) {
                    result = result.setBit(3)
                }

                // Bit 7 is sound status
                if (enabled) {
                    result = result.setBit(7)
                }
                result
            }
            else -> throw IllegalArgumentException("Address ${address.hex16()} does not belong to Sound")
        }
    }

    fun writeByte(address: Int, value: Int) {
        val newVal = value and 0xFF

        /* When powered off, all registers (NR10-NR51) are instantly written with zero and any writes to those
         * registers are ignored while power remains off (except on the DMG, where length counters are
         * unaffected by power and can still be written while off)
         */
        if (!enabled) {
            if (address != MMU.NR52 && address != MMU.NR11 && address != MMU.NR21 && address != MMU.NR31 && address != MMU.NR41) {
                return
            }
        }

        when (address) {
            MMU.NR10,
            MMU.NR11,
            MMU.NR12,
            MMU.NR13,
            MMU.NR14 -> {
                square1.writeByte(address, value)
            }
            MMU.NR21,
            MMU.NR22,
            MMU.NR23,
            MMU.NR24 -> {
                square2.writeByte(address, value)
            }
            MMU.NR30,
            MMU.NR31,
            MMU.NR32,
            MMU.NR33,
            MMU.NR34,
            in 0xFF30..0xFF3F -> {
                wave.writeByte(address, value)
            }
            MMU.NR41,
            MMU.NR42,
            MMU.NR43,
            MMU.NR44 -> {
                noise.writeByte(address, value)
            }
            MMU.NR50 -> {
                this.vinLeft = newVal.isBit(7)
                this.volumeLeft = (newVal shr 4) and 0b111
                this.vinRight = newVal.isBit(3)
                this.volumeRight = newVal and 0b111
            }
            MMU.NR51 -> {
                this.leftEnables[3] = newVal.isBit(7)
                this.leftEnables[2] = newVal.isBit(6)
                this.leftEnables[1] = newVal.isBit(5)
                this.leftEnables[0] = newVal.isBit(4)
                this.rightEnables[3] = newVal.isBit(3)
                this.rightEnables[2] = newVal.isBit(2)
                this.rightEnables[1] = newVal.isBit(1)
                this.rightEnables[0] = newVal.isBit(0)
            }
            MMU.NR52 -> {
                val wasEnabled = enabled
                enabled = value.isBit(7)

                if (wasEnabled && !enabled) {
                    for (channel in allChannels) {
                        channel.powerOff()
                    }

                    vinLeft = false
                    volumeLeft = 0
                    vinRight = false
                    volumeRight = 0

                    for (i in 0..3) {
                        this.leftEnables[i] = false
                        this.rightEnables[i] = false
                    }
                }

                if (!wasEnabled && enabled) {
                    for (channel in allChannels) {
                        channel.powerOn()
                    }
                }
            }
            else -> throw IllegalArgumentException("Address ${address.hex16()} does not belong to Sound")
        }
    }

}