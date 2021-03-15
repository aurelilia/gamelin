/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:02 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class FrequencySweep {

    // sweep parameters
    private var period = 0
    private var negate = false
    private var shift = 0

    // current process variables
    private var timer = 0
    private var shadowFreq = 0

    var nr13 = 0
    var nr14 = 0
        set(value) {
            field = value
            if (value and (1 shl 7) != 0) trigger()
        }

    private var i = 0
    private var overflow = false
    private var counterEnabled = false
    private var negging = false

    fun start() {
        counterEnabled = false
        i = 8192
    }

    fun trigger() {
        negging = false
        overflow = false
        shadowFreq = nr13 or (nr14 and 7 shl 8)
        timer = if (period == 0) 8 else period
        counterEnabled = period != 0 || shift != 0
        if (shift > 0) {
            calculate()
        }
    }

    fun setNr10(value: Int) {
        period = value shr 4 and 7
        negate = value.isBit(3)
        shift = value and 7
        if (negging && !negate) overflow = true
    }

    fun cycle() {
        if (++i == DIVIDER) {
            i = 0
            if (!counterEnabled) return
            if (--timer == 0) {
                timer = if (period == 0) 8 else period
                if (period != 0) {
                    val newFreq = calculate()
                    if (!overflow && shift != 0) {
                        shadowFreq = newFreq
                        nr13 = shadowFreq and 0xff
                        nr14 = shadowFreq and 0x700 shr 8
                        calculate()
                    }
                }
            }
        }
    }

    private fun calculate(): Int {
        var freq = shadowFreq shr shift
        if (negate) {
            freq = shadowFreq - freq
            negging = true
        } else freq += shadowFreq
        if (freq > 2047) overflow = true
        return freq
    }

    fun isEnabled() = !overflow

    companion object {
        private const val DIVIDER: Int = CLOCK_SPEED_HZ / 128
    }
}
