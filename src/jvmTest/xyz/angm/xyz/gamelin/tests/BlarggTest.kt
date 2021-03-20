/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 9:28 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ktx.assets.file
import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.Debugger
import xyz.angm.gamelin.system.GameBoy
import java.io.FileFilter

private const val TEST_TIMEOUT_SECONDS = 60

class BlarggTest : FunSpec({
    // Debugger that reads serial output into a string.
    val debugger = object : Debugger() {

        val data = StringBuilder()
        var isRamOutput = false
        var ramTestSuccess = false
        var ramTestFail = false

        override fun writeOccurred(addr: Short, value: Byte) {
            val addr = addr.int()
            val value = value.int()
            when (addr) {
                // Serial (cpu tests)
                0xFF01 -> data.append(value.toChar())

                // RAM output (sound tests)
                0xA000 -> if (isRamOutput && value == 0x00) ramTestSuccess = true else if (isRamOutput && value != 0x80) ramTestFail = true
                0xA001 -> isRamOutput = value == 0xDE
                0xA002 -> isRamOutput = value == 0xB0
                0xA003 -> isRamOutput = value == 0x61
            }
        }

        fun success() = data.contains("Passed") or ramTestSuccess
        fun failed() = data.contains("Failed") or ramTestFail

        fun reset() {
            data.clear()
            isRamOutput = false
            ramTestSuccess = false
            ramTestFail = false
        }

        override fun dispose() {}
    }
    val gb = GameBoy(debugger)

    for (dir in file("roms/test/blargg").list(FileFilter { it.isDirectory && !it.name.contains("disabled") })) {
        context(dir.name()) {
            for (test in dir.list(FileFilter { it.extension == "gb" })) {
                test(test.name()) {
                    gb.loadGame(test.readBytes())
                    gb.skipBios()
                    debugger.reset()

                    for (i in 0 until TEST_TIMEOUT_SECONDS) {
                        gb.advanceDelta(1f)
                        if (debugger.success()) break
                        else if (debugger.failed()) fail(debugger.data.toString())
                    }

                    debugger.success() shouldBe true
                }
            }
        }
    }
})