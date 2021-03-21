/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:32 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx

/** Debugger that will write CPU log to a file. */
class DesktopDebugger : Debugger() {

    private val logFile = Gdx.files.local("gamelin.cpu.log")

    override fun flushLog() {
        logFile.writeString(logger.toString(), true)
        super.flushLog()
    }
}