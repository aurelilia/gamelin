/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 12:58 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.interfaces.AudioOutput
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.io.IODevice
import xyz.angm.gamelin.system.io.MMU

/** The APU of the GameBoy, containg all 4 channels and responsible for providing the output with audio.
 * This implementation is abridged from stan-roelofs's emulator: https://github.com/stan-roelofs/Kotlin-Gameboy-Emulator
 * Who probably abridged it from trekawek's coffee-gb: https://github.com/trekawek/coffee-gb
 * Thank you to both stan-roelofs and trekawek! */
class APU(private val gb: GameBoy) : IODevice() {

    var output = AudioOutput()
    private var outDiv = 0

    private val square1 = SquareWave1()
    private val square2 = SquareWave2()
    private val wave = WaveChannel()
    private val noise = NoiseChannel()

    private val channels: Array<SoundChannel> = arrayOf(square1, square2, wave, noise)
    private var enabled = true

    private var vinLeft = false
    private var vinRight = false
    private var volumeLeft = 0b111
    private var volumeRight = 0b111
    private var leftEnables = BooleanArray(4) { false }
    private var rightEnables = BooleanArray(4) { it < 2 }

    fun reset() {
        vinLeft = false
        vinRight = false
        volumeLeft = 0b111
        volumeRight = 0b111

        leftEnables.fill(true)
        for (i in 0 until 4) rightEnables[i] = i < 2

        for (channel in channels) {
            channel.reset()
        }

        enabled = true
        output.reset()
    }

    fun step(tCycles: Int) {
        outDiv += tCycles
        if (outDiv < DIVIDER) {
            for (i in 0 until 4) channels[i].cycle(tCycles)
        } else {
            outDiv -= DIVIDER
            var left = 0
            var right = 0

            for (i in 0 until 4) {
                val sample = channels[i].cycle(tCycles)
                if (leftEnables[i]) left += sample
                if (rightEnables[i]) right += sample
            }

            left *= volumeLeft + 1
            right *= volumeRight + 1
            left /= 4
            right /= 4
            output.play(left.toByte(), right.toByte())
        }
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            in MMU.NR10..MMU.NR14 -> square1.readByte(addr)
            in MMU.NR21..MMU.NR24 -> square2.readByte(addr)
            in MMU.NR30..MMU.NR34, in MMU.WAVE_SAMPLES -> wave.readByte(addr)
            in MMU.NR41..MMU.NR44 -> noise.readByte(addr)
            MMU.NR50 -> ((volumeRight) or (volumeLeft shl 4)).setBit(7, vinLeft).setBit(3, vinRight)
            MMU.NR51 -> {
                var result = 0
                rightEnables.forEachIndexed { i, en -> result = result.setBit(i, en) }
                leftEnables.forEachIndexed { i, en -> result = result.setBit(i + 4, en) }
                result
            }
            MMU.NR52 -> {
                // Bits 0-3 are statuses of channels (1, 2, wave, noise)
                var result = 0b01110000 // Bits 4-6 are unused
                if (square1.enabled) result = result.setBit(0)
                if (square2.enabled) result = result.setBit(1)
                if (wave.enabled) result = result.setBit(2)
                if (noise.enabled) result = result.setBit(3)
                // Bit 7 is sound status
                if (enabled) result = result.setBit(7)
                result
            }
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        // When powered off, all registers (NR10-NR51) are instantly written with zero and any writes to those
        // registers are ignored while power remains off (except on the DMG, where length counters are
        // unaffected by power and can still be written while off)
        if (!enabled && !gb.cgbMode && addr != MMU.NR52 && addr != MMU.NR11 && addr != MMU.NR21 && addr != MMU.NR31 && addr != MMU.NR41) {
            return
        }

        when (addr) {
            in MMU.NR10..MMU.NR14 -> square1.writeByte(addr, value)
            in MMU.NR21..MMU.NR24 -> square2.writeByte(addr, value)
            in MMU.NR30..MMU.NR34, in MMU.WAVE_SAMPLES -> wave.writeByte(addr, value)
            in MMU.NR41..MMU.NR44 -> noise.writeByte(addr, value)
            MMU.NR50 -> {
                vinLeft = value.isBit(7)
                vinRight = value.isBit(3)
                volumeLeft = (value shr 4) and 0b111
                volumeRight = value and 0b111
            }
            MMU.NR51 -> {
                for (i in 0 until 4) {
                    this.leftEnables[i] = value.isBit(i + 4)
                    this.rightEnables[i] = value.isBit(i)
                }
            }
            MMU.NR52 -> {
                val wasEnabled = enabled
                enabled = value.isBit(7)

                if (wasEnabled && !enabled) {
                    for (channel in channels) channel.powerOff()
                    vinLeft = false
                    volumeLeft = 0
                    vinRight = false
                    volumeRight = 0
                    leftEnables.fill(false)
                    rightEnables.fill(false)
                }

                if (!wasEnabled && enabled) {
                    for (channel in channels) channel.powerOn()
                }
            }
        }
    }

    companion object {
        /** The amount of ticks that need to pass per sample sent to the output. */
        val DIVIDER = GameBoy.T_CLOCK_HZ / AudioOutput.SAMPLE_RATE
    }
}