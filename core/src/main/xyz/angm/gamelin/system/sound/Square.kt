/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:15 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.GameBoy

open class Square(gb: GameBoy, offs: Int) : Channel(gb, offs, 64) {

    protected var freqDivider = 0
    protected var lastOutput = 0
    protected var i = 0
    protected var volumeEnvelope = VolumeEnvelope()

    override var nr1
        get() = super.nr1
        set(value) {
            super.nr1 = value
            length.setLength(64 - (value and 63))
        }
    override var nr2
        get() = super.nr2
        set(value) {
            super.nr2 = value
            volumeEnvelope.setNr2(value)
            dacEnabled = value and 248 != 0
            channelEnabled = channelEnabled and dacEnabled
        }

    override fun start() {
        i = 0
        length.start()
        volumeEnvelope.start()
    }

    override fun trigger() {
        i = 0
        freqDivider = 1
        volumeEnvelope.trigger()
    }

    override fun cycle(): Int {
        volumeEnvelope.cycle()
        if (!getE()) return 0

        if (--freqDivider == 0) {
            resetFreqDivider()
            lastOutput = getDuty() and (1 shl i) shr i
            i = (i + 1) % 8
        }
        return lastOutput * volumeEnvelope.getVolume()
    }

    protected open fun getE(): Boolean {
        var e = true
        e = updateLength() && e
        e = dacEnabled && e
        return e
    }

    protected fun getDuty(): Int {
        return when (nr1 shr 6) {
            0 -> 1
            1 -> 129
            2 -> 135
            3 -> 126
            else -> throw IllegalStateException()
        }
    }

    protected fun resetFreqDivider() {
        freqDivider = getFrequency() * 4
    }
}

class Square1(gb: GameBoy) : Square(gb, 0xFF10) {

    private val frequencySweep = FrequencySweep()

    override var nr0
        get() = super.nr0
        set(value) {
            super.nr0 = value
            frequencySweep.setNr10(value)
        }
    override var nr3
        get() = frequencySweep.nr13
        set(value) {
            super.nr3 = value
            frequencySweep.nr13 = value
        }
    override var nr4
        get() = super.nr4 and 248 or (frequencySweep.nr14 and 7)
        set(value) {
            super.nr4 = value
            frequencySweep.nr14 = value
        }

    override fun start() {
        i = 0
        length.start()
        frequencySweep.start()
        volumeEnvelope.start()
    }

    override fun getE(): Boolean {
        var e = true
        e = updateLength() && e
        e = updateSweep() && e
        e = dacEnabled && e
        return e
    }

    private fun updateSweep(): Boolean {
        frequencySweep.cycle()
        if (channelEnabled && !frequencySweep.isEnabled()) channelEnabled = false
        return channelEnabled
    }
}

class Square2(gb: GameBoy) : Square(gb, 0xFF15)