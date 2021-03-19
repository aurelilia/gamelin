/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/19/21, 9:56 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

expect object FileSystem {
    fun saveRAM(game: String, ram: ByteArray)
    fun loadRAM(game: String): ByteArray?
}