/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:28 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import kotlinx.browser.localStorage
import xyz.angm.gamelin.bytesToString
import xyz.angm.gamelin.stringToBytes

/** File system that stores game saves in browser local storage. */
actual object FileSystem {
    actual fun saveRAM(game: String, ram: ByteArray) = localStorage.setItem(game, bytesToString(ram))
    actual fun loadRAM(game: String) = localStorage.getItem(game)?.let { stringToBytes(it) }
}