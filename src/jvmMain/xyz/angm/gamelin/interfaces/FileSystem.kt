/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 8:13 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.files.FileHandle
import xyz.angm.gamelin.interfaces.FileSystem.gamePath

/** FileSystem that saves game RAM next to the game in a .sav file.
 * [gamePath] needs to be set to the location of the current game. */
actual object FileSystem {

    var gamePath: FileHandle? = null

    actual fun saveRAM(game: String, ram: ByteArray) {
        saveFileHandle()?.writeBytes(ram, false)
    }

    actual fun loadRAM(game: String): ByteArray? {
        val file = saveFileHandle()
        return if (file?.exists() == true) file.readBytes() else null
    }

    private fun saveFileHandle(): FileHandle? {
        val path = gamePath ?: return null
        val directory = path.parent()
        return directory.child("${path.nameWithoutExtension()}.sav")
    }
}