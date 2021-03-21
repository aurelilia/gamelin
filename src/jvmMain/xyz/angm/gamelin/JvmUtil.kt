/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 3:35 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

actual fun Number.hex8() = String.format("0x%02X", this)
actual fun Number.hex16() = String.format("0x%04X", this)

private val json = run {
    val j = Json()
    j.setOutputType(JsonWriter.OutputType.json)
    j.setUsePrototypes(false)
    j
}

data class FullConfiguration(val gamelin: Configuration = Configuration(), val desktop: DesktopConfiguration = DesktopConfiguration())

actual fun saveConfiguration() {
    Gdx.files.local("gamelin.config.json").writeString(json.prettyPrint(FullConfiguration(configuration, desktopConfiguration)), false)
}

actual fun loadConfiguration() = loadFullConfig()?.gamelin ?: Configuration()
fun loadDesktopConfiguration() = loadFullConfig()?.desktop ?: DesktopConfiguration()

private fun loadFullConfig(): FullConfiguration? {
    val file = Gdx.files.local("gamelin.config.json")
    return if (file.exists()) json.fromJson(FullConfiguration::class.java, file)
    else null
}
