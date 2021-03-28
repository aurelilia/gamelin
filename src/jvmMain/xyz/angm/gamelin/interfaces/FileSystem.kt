/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 1:02 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.files.FileHandle
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.interfaces.FileSystem.gamePath
import xyz.angm.gamelin.runInGbThread
import xyz.angm.gamelin.system.GameBoy

/** FileSystem that saves game RAM next to the game in a .sav file, RTC in .rtc,
 * and save states in .state files.
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

    fun saveState(slot: String, console: GameBoy = gb) {
        runInGbThread {
            val out = Output(saveFileHandle("$slot.state")?.write(false) ?: return@runInGbThread)
            SaveState.saveState(out, console)
            out.close()
        }
    }

    fun loadState(slot: String) {
        val file = saveFileHandle("$slot.state")
        if (file?.exists() != true) return
        val input = Input(file.read())
        saveState("last")
        SaveState.loadState(input)
    }

    private fun saveFileHandle(ext: String = "sav"): FileHandle? {
        val path = gamePath ?: return null
        val directory = path.parent()
        return directory.child("${path.nameWithoutExtension()}.$ext")
    }
}

actual fun timeInSeconds() = System.currentTimeMillis() / 1000L
