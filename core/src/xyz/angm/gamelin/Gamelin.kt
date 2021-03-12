/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 1:35 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import xyz.angm.gamelin.system.GameBoy
import kotlin.system.exitProcess

/** The emulator. */
open class Gamelin : ApplicationAdapter() {

    private val stage = Stage(ScreenViewport())
    private val gb = GameBoy()

    override fun create() {
        VisUI.load()
    }

    override fun render() {
        gb.advance()
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() = exitProcess(0)
}
