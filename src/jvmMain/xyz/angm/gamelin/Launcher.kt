/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 10:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** The LWJGL configuration used for the game */
val configuration = Lwjgl3ApplicationConfiguration()

/** The game instance */
val game = Gamelin()

/** Initialize and launch the game. */
fun main() {
    Thread.setDefaultUncaughtExceptionHandler(::handleException)
    setConfiguration()
    Lwjgl3Application(game, configuration)
}

/** Handle exceptions */
private fun handleException(thread: Thread, throwable: Throwable) {
    Gdx.app?.exit()

    System.err.println("Whoops. This shouldn't have happened...")
    System.err.println("Exception in thread ${thread.name}:\n")
    throwable.printStackTrace()
    System.err.println("Gamelin is exiting.")

    val builder = StringBuilder()
    builder.append("The emulator encountered an exception, and is forced to close.\n")
    builder.append("Exception: ${throwable.javaClass.name}: ${throwable.localizedMessage}\n")
    builder.append("For more information, see the console output or log.")

    showDialog(builder.toString(), JOptionPane.ERROR_MESSAGE)
    exitProcess(-1)
}

/** Simple method for showing a dialog. Type should be a type from JOptionPane */
private fun showDialog(text: String, type: Int) = JOptionPane.showMessageDialog(null, text, "Gamelin", type)

/** Returns the LWJGL configuration. */
private fun setConfiguration() {
    configuration.setIdleFPS(60)
    configuration.useVsync(true)
    configuration.setTitle("Gamelin")
}