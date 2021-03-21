/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:01 PM.
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
    /** Load RAM; return null if it does not exist */
    fun loadRAM(game: String): ByteArray?
}