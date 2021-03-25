/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 6:52 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import com.badlogic.gdx.files.FileHandle
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContextScope
import io.kotest.matchers.shouldBe
import ktx.assets.file
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.Reg
import java.io.FileFilter

/** The amount of time to run the system for before considering a test to have timeouted/failed. */
private const val TEST_TIMEOUT_SECONDS = 10
private val gb = GameBoy(preferCGB = false)

/** Test runner for all mooneye-gb tests at assets/roms/test/mooneye. */
class MooneyeTest : FunSpec({
    for (dir in file("roms/test/mooneye").list(FileFilter { it.isDirectory })) {
        context(dir.name()) {
            runDir(dir)
        }
    }
})

suspend fun FunSpecContextScope.runDir(dir: FileHandle) {
    for (file in dir.list()) {
        if (file.name().contains("disabled") && !ProjectConfig.TEST_ALL) continue
        if (file.extension() == "gb") {
            test(file.name()) {
                gb.loadGame(file.readBytes())

                for (i in 0 until TEST_TIMEOUT_SECONDS) {
                    gb.advanceDelta(1f)
                    if (gb.mooneyeFinished()) break
                }

                val success = gb.mooneyeFinished()
                success shouldBe true
            }
        } else if (file.isDirectory) {
            context(file.name()) {
                runDir(file)
            }
        }
    }
}

/** All mooneye-gb tests set registers to these values to indicate test success. */
fun GameBoy.mooneyeFinished() = this.read(Reg.A) == 0 && this.read(Reg.B) == 3
        && this.read(Reg.C) == 5 && this.read(Reg.D) == 8
        && this.read(Reg.E) == 13 && this.read(Reg.H) == 21
        && this.read(Reg.L) == 34
