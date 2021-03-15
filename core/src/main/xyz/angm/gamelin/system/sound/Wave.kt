/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:16 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.GameBoy

class Wave(gb: GameBoy) : Channel(gb, 0xFF1A, 256) {

    private val DMG_WAVE = intArrayOf(
        0x84, 0x40, 0x43, 0xaa, 0x2d, 0x78, 0x92, 0x3c,
        0x60, 0x59, 0x59, 0xb0, 0x34, 0xb8, 0x2e, 0xda
    )

    private val CGB_WAVE = intArrayOf(
        0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff,
        0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff
    )

    private var freqDivider = 0
    private var lastOutput = 0
    private var i = 0
    private var ticksSinceRead = 65536
    private var lastReadAddr = 0
    private var buffer = 0
    private var triggered = false

    override var nr0
        get() = super.nr0
        set(value) {
            super.nr0 = value
            dacEnabled = value and (1 shl 7) != 0
            channelEnabled = channelEnabled and dacEnabled
        }
    override var nr1
        get() = super.nr1
        set(value) {
            super.nr1 = value
            length.setLength(256 - value)
        }
    override var nr4
        get() = super.nr4
        set(value) {
            if (value and (1 shl 7) != 0 && isEnabled() && freqDivider == 2) {
                var pos = i / 2
                if (pos < 4) {
                    gb.write(0xff30, gb.read(0xff30 + pos))
                } else {
                    pos = pos and 3.inv()
                    for (j in 0..3) {
                        gb.write(0xff30 + j, gb.read(0xff30 + (pos + j) % 0x10))
                    }
                }
            }
            super.nr4 = value
        }

    override fun start() {
        i = 0
        buffer = 0
        length.start()
    }

    override fun trigger() {
        i = 0
        freqDivider = 6
    }

    override fun cycle(): Int {
        ticksSinceRead++
        if (!updateLength()) {
            return 0
        }
        if (!dacEnabled) {
            return 0
        }
        if (nr0 and (1 shl 7) === 0) {
            return 0
        }
        if (--freqDivider == 0) {
            resetFreqDivider()
            if (triggered) {
                lastOutput = buffer shr 4 and 0x0f
                triggered = false
            } else {
                lastOutput = getWaveEntry()
            }
            i = (i + 1) % 32
        }
        return lastOutput
    }

    private fun getVolume() = nr2 ushr 5 and 3

    private fun getWaveEntry(): Int {
        ticksSinceRead = 0
        lastReadAddr = 0xff30 + i / 2
        buffer = gb.read(lastReadAddr)
        var b = buffer
        b = if (i % 2 == 0) {
            b shr 4 and 0x0f
        } else {
            b and 0x0f
        }
        return when (getVolume()) {
            0 -> 0
            1 -> b
            2 -> b shr 1
            3 -> b shr 2
            else -> throw IllegalStateException()
        }
    }

    private fun resetFreqDivider() {
        freqDivider = getFrequency() * 2
    }
}