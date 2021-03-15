/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:17 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.GameBoy

class Noise(gb: GameBoy) : Channel(gb, 0xFF1F, 64) {

    private var volumeEnvelope = VolumeEnvelope()
    private var polynomialCounter = PolynomialCounter()
    private var lastResult = 0
    private val lfsr = Lfsr()

    override var nr1
        get() = super.nr1
        set(value) {
            super.nr0 = value
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
    override var nr3
        get() = super.nr3
        set(value) {
            super.nr3 = value
            polynomialCounter.setNr43(value)
        }

    override fun start() {
        length.start()
        lfsr.start()
        volumeEnvelope.start()
    }

    override fun trigger() {
        lfsr.reset()
        volumeEnvelope.trigger()
    }

    override fun cycle(): Int {
        volumeEnvelope.cycle()
        if (!updateLength()) {
            return 0
        }
        if (!dacEnabled) {
            return 0
        }
        if (polynomialCounter.tick()) {
            lastResult = lfsr.nextBit(nr3 and (1 shl 3) != 0)
        }
        return lastResult * volumeEnvelope.getVolume()
    }
}