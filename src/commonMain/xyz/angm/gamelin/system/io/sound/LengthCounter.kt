/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:15 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class LengthCounter(private val fullLength: Int, private val soundChannel: SoundChannel) {

    private var length = 0
    private var counter = 0
    var lengthEnabled = false
        private set

    init {
        reset()
    }

    fun reset() {
        this.lengthEnabled = false
        this.counter = 0
        this.length = fullLength
    }

    fun cycle(cycles: Int) {
        counter += cycles
        if (counter >= DIVIDER) {
            counter -= DIVIDER
            if (lengthEnabled && length > 0) {
                decreaseLength()
            }
        }
    }

    fun setNr1(value: Int) {
        length = if (value == 0) fullLength else fullLength - value
    }

    fun setNr4(value: Int) {
        val wasEnabled = lengthEnabled
        lengthEnabled = value.isBit(6)

        /* Extra length clocking occurs when writing to NRx4 when the frame sequencer's next step is one that doesn't clock the length counter.
         * In this case, if the length counter was PREVIOUSLY disabled and now enabled and the length counter is not zero, it is decremented.
         */
        if (!wasEnabled && lengthEnabled && length != 0 && counter < DIVIDER / 2) {
            decreaseLength()
        }

        if (value.isBit(7) && length == 0) {
            length = fullLength

            /* If a channel is triggered when the frame sequencer's next step is one that doesn't clock the length counter and the
             * length counter is now enabled and length is being set to 64 (256 for wave channel) because it was previously zero,
             * it is set to 63 instead (255 for wave channel).
             */
            if (counter < DIVIDER / 2 && lengthEnabled) {
                decreaseLength()
            }
        }
    }

    private fun decreaseLength() {
        length--
        if (length == 0) {
            soundChannel.enabled = false
        }
    }

    companion object {
        private const val DIVIDER = CLOCK_SPEED_HZ / 256
    }
}