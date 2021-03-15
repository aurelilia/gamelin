/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 8:55 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class LengthCounter(private val fullLength: Int) {

    var value = 0
        private set
    private var i: Long = 0
    var isEnabled = false
        private set

    fun start() {
        i = 8192
    }

    fun cycle() {
        if (++i == DIVIDER) {
            i = 0
            if (isEnabled && value > 0) value--
        }
    }

    fun setLength(length: Int) {
        if (length == 0) {
            this.value = fullLength
        } else {
            this.value = length
        }
    }

    fun setNr4(value: Int) {
        val enable = value.isBit(6)
        val trigger = value.isBit(7)
        if (isEnabled) {
            if (this.value == 0 && trigger) {
                if (enable && i < DIVIDER / 2) setLength(fullLength - 1)
                else setLength(fullLength)
            }
        } else if (enable) {
            if (this.value > 0 && i < DIVIDER / 2) this.value--
            if (this.value == 0 && trigger && i < DIVIDER / 2) setLength(fullLength - 1)
        } else if (this.value == 0 && trigger) setLength(fullLength)
        isEnabled = enable
    }

    fun reset() {
        isEnabled = true
        i = 0
        value = 0
    }

    companion object {
        private const val DIVIDER = CLOCK_SPEED_HZ / 256.toLong()
    }
}