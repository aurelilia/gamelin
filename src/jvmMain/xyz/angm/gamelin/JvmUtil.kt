/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 1:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.GameBoy
import java.io.FileInputStream
import java.io.FileOutputStream

actual fun Number.hex8() = String.format("0x%02X", this)
actual fun Number.hex16() = String.format("0x%04X", this)

private val json = Json().apply {
    setOutputType(JsonWriter.OutputType.json)
    setUsePrototypes(false)
}

data class FullConfiguration(val gamelin: Configuration = Configuration(), val desktop: DesktopConfiguration = DesktopConfiguration())

actual fun saveConfiguration() {
    Gdx.files.local("gamelin.config.json").writeString(json.prettyPrint(FullConfiguration(configuration, config)), false)
}

actual fun loadConfiguration() = loadFullConfig()?.gamelin ?: Configuration()
fun loadDesktopConfiguration() = loadFullConfig()?.desktop ?: DesktopConfiguration()

private fun loadFullConfig(): FullConfiguration? {
    val file = Gdx.files.local("gamelin.config.json")
    return if (file.exists()) json.fromJson(FullConfiguration::class.java, file)
    else null
}

private val kryo = Kryo().apply {
    references = true
    isRegistrationRequired = false
    instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
}

/** Save the current global system to a file. */
fun saveGb() {
    val out = Output(FileOutputStream("gb.bin"))
    kryo.writeObject(out, gb)
    out.flush()
    out.close()
}

/** Read the file saved by [saveGb] into the global system, replacing it. */
fun loadGb() {
    val input = Input(FileInputStream("gb.bin"))
    val oldGb = gb
    gb = kryo.readObject(input, GameBoy::class.java)
    gb.mmu.ppu.renderer = TileRenderer(gb.mmu, 20, 18, 4f)
    oldGb.dispose()
}
