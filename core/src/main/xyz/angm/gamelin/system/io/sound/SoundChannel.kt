/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 6:53 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

abstract class SoundChannel {

    var enabled = false
    protected abstract val lengthCounter: LengthCounter
    protected val volumeEnvelope = VolumeEnvelope()
    protected var lastOutput = 0

    abstract fun tick(): Int

    open fun powerOn() {
        volumeEnvelope.powerOn()
    }

    open fun powerOff() {
        enabled = false
        lengthCounter.reset()
        volumeEnvelope.reset()
    }

    protected open fun trigger() {
        enabled = true
        volumeEnvelope.trigger()

        if (!volumeEnvelope.getDac()) {
            enabled = false
        }
    }

    open fun reset() {
        enabled = false
        lastOutput = 0
        lengthCounter.reset()
        volumeEnvelope.reset()
    }
}