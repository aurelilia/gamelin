/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 10:32 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.Cartridge

/** This class represents the file system interface used to persist a game's battery backed RAM by [Cartridge].
 * Games are identified by a string composed of their extended reported name and their target region. */
expect object FileSystem {
    /** Save given RAM to disk, overwriting any old saves */
    fun saveRAM(game: String, ram: ByteArray)
    /** Save given RTC clock to disk, overwriting any old ones */
    fun saveRTC(game: String, rtc: DateTime)
    /** Load RAM; return null if it does not exist */
    fun loadRAM(game: String): ByteArray?
    /** Load game RTC clock; return null if it does not exist */
    fun loadRTC(game: String): DateTime?
}

/** A specific point in time representing the RTC register of some MBC3 chips.
 * TODO: Persist latching
 * TODO: Proper timekeeping on set, it's not a reset */
class DateTime(private var start: Long = timeInSeconds()) {

    private var latchedAt: Long? = null

    fun latch() {
        latchedAt = timeInSeconds()
    }

    fun get(idx: Int) = (diff() / dividers[idx]) % modulo[idx]

    fun set(idx: Int, value: Int) {
        start = timeInSeconds() // TODO
    }

    private fun diff() = (latchedAt ?: timeInSeconds() - start).toInt()
    override fun toString() = start.toString()

    companion object {
        private val dividers = intArrayOf(1, 60, 3600, 86400)
        private val modulo = intArrayOf(60, 60, 24, 511)
    }
}

/** This method should return the current UNIX time divided by 1000. */
expect fun timeInSeconds(): Long