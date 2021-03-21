/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.sound

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.GameBoy

class VolumeEnvelope {

    private var add = false
    private var startingVolume = 0
    var volume = 0
    private var period = 0
    private var counter = 0
    private var enabled = false

    fun powerOn() {
        counter %= 8192
    }

    fun reset() {
        enabled = false
        counter = 0
        volume = 0
        startingVolume = 0
        add = false
        period = 0
    }

    fun cycle(cycles: Int) {
        counter += cycles
        if (period > 0 && counter >= period * DIVIDER) {
            counter = 0

            if (enabled && period > 0) {
                val newVolume = if (add) {
                    volume + 1
                } else {
                    volume - 1
                }

                if (newVolume < 0 || newVolume > 15) {
                    enabled = false
                } else {
                    volume = newVolume
                }
            }
        }
    }

    fun setNr2(value: Int) {
        startingVolume = (value shr 4) and 0b1111
        add = value.isBit(3)
        period = value and 0b111
    }

    fun getNr2(): Int {
        var result = this.startingVolume shl 4
        result = result.setBit(3, add)
        result = result or period
        return result
    }

    // If any of the upper 5 bits is enabled, dac is enabled
    fun getDac() = (getNr2() and 0b11111000) != 0

    fun trigger() {
        volume = startingVolume
        enabled = true
    }

    companion object {
        private const val DIVIDER = GameBoy.T_CLOCK_HZ / 64
    }
}