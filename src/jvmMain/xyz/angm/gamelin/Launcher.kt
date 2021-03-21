/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:40 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** The emulator instance */
val emu = Gamelin()

/** Initialize and launch the emulator. */
fun main() {
    Thread.setDefaultUncaughtExceptionHandler(::handleException)
    val configuration = Lwjgl3ApplicationConfiguration()
    configuration.setIdleFPS(15)
    configuration.useVsync(true)
    configuration.setTitle("Gamelin")
    Lwjgl3Application(emu, configuration)
}

/** Handle exceptions */
private fun handleException(thread: Thread, throwable: Throwable) {
    Gdx.app?.exit()

    System.err.println("Whoops. This shouldn't have happened...")
    System.err.println("Exception in thread ${thread.name}:\n")
    throwable.printStackTrace()
    System.err.println("Gamelin is exiting.")

    val builder = StringBuilder()
    builder.appendLine("The emulator encountered an exception, and is forced to close.")
    builder.appendLine("For more information, see the console output or log.")
    builder.appendLine("Exception: ${throwable.javaClass.name}: ${throwable.localizedMessage}")

    JOptionPane.showMessageDialog(null, builder.toString(), "Gamelin", JOptionPane.ERROR_MESSAGE)
    exitProcess(-1)
}
