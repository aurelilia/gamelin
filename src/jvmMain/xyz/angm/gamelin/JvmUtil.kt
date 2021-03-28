/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 7:08 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

actual fun Number.hex8() = String.format("0x%02X", this)
actual fun Number.hex16() = String.format("0x%04X", this)

private val json = Json().apply {
    setOutputType(JsonWriter.OutputType.json)
    setUsePrototypes(false)
    ignoreUnknownFields = true
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

/** Run the given runnable in the GB thread if the GB is running.
 * If not, it'll be executed on the current thread immediately
 * (this is fine since it's run on the GB thread to solve synchronization issues,
 * which cannot happen while it isn't running). */
fun runInGbThread(run: () -> Unit) {
    if (gb.debugger.emuHalt) run()
    else gb.mmu.ppu.renderer.queueRunnable(run)
}
