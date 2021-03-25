/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 10:16 PM.
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

    actual fun saveRTC(game: String, rtc: DateTime) {
        saveFileHandle("rtc")?.writeString(rtc.toString(), false)
    }

    actual fun loadRAM(game: String): ByteArray? {
        val file = saveFileHandle()
        return if (file?.exists() == true) file.readBytes() else null
    }

    actual fun loadRTC(game: String): DateTime? {
        val file = saveFileHandle("rtc")
        return if (file?.exists() == true) DateTime(file.readString().toLong()) else null
    }

    private fun saveFileHandle(ext: String = "sav"): FileHandle? {
        val path = gamePath ?: return null
        val directory = path.parent()
        return directory.child("${path.nameWithoutExtension()}.$ext")
    }
}

actual fun timeInSeconds() = System.currentTimeMillis() / 1000L