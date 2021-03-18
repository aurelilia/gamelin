/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:18 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.spec.AutoScan

@AutoScan
object LibGDXSetup : ProjectListener {
    override suspend fun beforeProject() {
        HeadlessApplication(object : ApplicationAdapter() {})
    }
}