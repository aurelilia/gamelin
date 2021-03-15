/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:03 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class VolumeEnvelope {

    private var initialVolume = 0
    private var envelopeDirection = 0
    private var sweep = 0
    private var volume = 0
    private var i = 0
    private var finished = false

    fun setNr2(register: Int) {
        initialVolume = register shr 4
        envelopeDirection = if (register and (1 shl 3) == 0) -1 else 1
        sweep = register and 7
    }

    fun isEnabled() = sweep > 0

    fun start() {
        finished = true
        i = 8192
    }

    fun trigger() {
        volume = initialVolume
        i = 0
        finished = false
    }

    fun cycle() {
        if (finished) return
        if (volume == 0 && envelopeDirection == -1 || volume == 15 && envelopeDirection == 1) {
            finished = true
            return
        }
        if (++i == sweep * CLOCK_SPEED_HZ / 64) {
            i = 0
            volume += envelopeDirection
        }
    }

    fun getVolume() = if (isEnabled()) volume else initialVolume
}
