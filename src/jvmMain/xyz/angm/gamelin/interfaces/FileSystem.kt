/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 7:02 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.files.FileHandle
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.interfaces.FileSystem.gamePath
import xyz.angm.gamelin.runInGbThread
import xyz.angm.gamelin.system.GameBoy

/** FileSystem that saves game RAM next to the game in a .sav file, RTC in .rtc,
 * and save states in .state files.
 * [gamePath] needs to be set to the location of the current game. */
actual object FileSystem {

    private val kryo = Kryo().apply {
        references = true
        isRegistrationRequired = false
        instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
    }
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
            kryo.writeObject(out, console)

            out.flush()
            out.close()
        }
    }

    fun loadState(slot: String) {
        val file = saveFileHandle("$slot.state")
        if (file?.exists() != true) return
        val input = Input(file.read())
        val oldGb = gb
        saveState("last", oldGb)

        val newGb = kryo.readObject(input, GameBoy::class.java)
        newGb.mmu.ppu.renderer = oldGb.mmu.ppu.renderer
        newGb.mmu.ppu.restore()
        newGb.mmu.cart.rom = oldGb.mmu.cart.rom
        newGb.debugger = oldGb.debugger
        gb = newGb

        // Don't actually call the dispose() method, all of the
        // native resources are carried to the new GB so it's fine
        oldGb.disposed = true
    }

    private fun saveFileHandle(ext: String = "sav"): FileHandle? {
        val path = gamePath ?: return null
        val directory = path.parent()
        return directory.child("${path.nameWithoutExtension()}.$ext")
    }
}

actual fun timeInSeconds() = System.currentTimeMillis() / 1000L
