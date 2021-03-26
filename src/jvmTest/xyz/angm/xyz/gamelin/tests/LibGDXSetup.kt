/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 3:50 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.spec.AutoScan
import xyz.angm.gamelin.configuration

@AutoScan
@Suppress("unused")
object LibGDXSetup : ProjectListener {
    override suspend fun beforeProject() {
        // Ensure we're running tests in DMG mode, some mooneye and blargg tests require it
        // and the bootrom takes less time
        configuration.preferCGB = false

        // Initialize libGDX to allow the tests to use some of the
        // desktop frontend
        HeadlessApplication(object : ApplicationAdapter() {})
    }
}