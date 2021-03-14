/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 10:34 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContainIgnoringCase
import ktx.assets.file
import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.Debugger
import xyz.angm.gamelin.system.GameBoy
import java.io.FileFilter

private const val TEST_TIMEOUT_SECONDS = 30

class BlarggTest : FunSpec({
    // Debugger that reads serial output into a string.
    val debugger = object : Debugger() {
        val data = StringBuilder()

        override fun writeOccured(addr: Short, value: Byte) {
            if (addr.int() == 0xFF01) data.append(value.toChar())
        }

        fun finish(): String {
            val str = data.toString()
            data.clear()
            return str
        }

        override fun dispose() {}
    }

    for (dir in file("roms/test/blargg").list(FileFilter { it.isDirectory && !it.name.contains("disabled") })) {
        context(dir.name()) {
            for (test in dir.list(FileFilter { it.extension == "gb" })) {
                test(test.name()) {
                    val gb = GameBoy(test.readBytes(), debugger)

                    for (i in 0 until TEST_TIMEOUT_SECONDS * 60) {
                        gb.advanceDelta(1 / 60f)
                        if (debugger.data.contains("Passed")) break
                        else if (debugger.data.contains("Failed")) fail(debugger.finish())
                    }

                    val res = debugger.finish()
                    gb.dispose()
                    res shouldContainIgnoringCase "Passed"
                }
            }
        }
    }
})