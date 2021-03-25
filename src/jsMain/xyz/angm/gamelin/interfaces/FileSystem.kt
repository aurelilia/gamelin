/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 10:16 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import kotlinx.browser.localStorage
import xyz.angm.gamelin.bytesToString
import xyz.angm.gamelin.stringToBytes
import kotlin.js.Date

/** File system that stores game saves in browser local storage. */
actual object FileSystem {
    actual fun saveRAM(game: String, ram: ByteArray) = localStorage.setItem(game, bytesToString(ram))
    actual fun saveRTC(game: String, rtc: DateTime) = localStorage.setItem("$game-rtc", rtc.toString())
    actual fun loadRAM(game: String) = localStorage.getItem(game)?.let { stringToBytes(it) }
    actual fun loadRTC(game: String) = localStorage.getItem("$game-rtc")?.toLong()?.let { DateTime(it) }
}

actual fun timeInSeconds() = Date().getTime().toLong() / 1000L