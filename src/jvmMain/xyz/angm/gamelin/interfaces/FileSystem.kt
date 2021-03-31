/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/31/21, 7:57 PM.
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
import java.lang.Exception

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

    /** Load a save state slot. Returns success. */
    fun loadState(slot: String): LoadStateStatus {
        val file = saveFileHandle("$slot.state")
        if (file?.exists() != true) return LoadStateStatus.FileNotFound
        val input = Input(file.read())
        saveState("last")
        return try {
            SaveState.loadState(input)
            LoadStateStatus.Success
        } catch (e: Exception) {
            LoadStateStatus.InvalidFile
        }
    }

    private fun saveFileHandle(ext: String = "sav"): FileHandle? {
        val path = gamePath ?: return null
        val directory = path.parent()
        return directory.child("${path.nameWithoutExtension()}.$ext")
    }

    enum class LoadStateStatus { Success, FileNotFound, InvalidFile }
}

actual fun timeInSeconds() = System.currentTimeMillis() / 1000L
