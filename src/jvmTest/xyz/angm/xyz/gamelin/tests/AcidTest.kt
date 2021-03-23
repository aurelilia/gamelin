/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/23/21, 10:45 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ktx.assets.file
import xyz.angm.gamelin.configuration
import xyz.angm.gamelin.system.GameBoy

/** The amount of time to run the system for before taking the comparison screenshot. */
private const val TEST_TIMEOUT_SECONDS = 5

/** Test runner for mattcurie's acid2 tests at assets/roms/test/acid2. */
class AcidTest : FunSpec({
    val tests = arrayOf("dmg-acid2.gb", "cgb-acid2.gbc")
    val gb = GameBoy()

    // Disable color correction in case the user enabled it; reference images do not have it
    configuration.cgbColorCorrection = false

    for (test in tests) {
        test(test) {
            val file = file("roms/test/acid2/$test")
            gb.loadGame(file.readBytes())

            for (i in 0 until TEST_TIMEOUT_SECONDS) {
                gb.advanceDelta(1f)
            }

            gb.mmu.ppu.renderer.compareTo(file("roms/test/acid2/$test.expected.png")) shouldBe true
        }
    }
})
