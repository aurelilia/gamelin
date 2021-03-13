/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 8:02 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.VisUI
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.windows.BGMapViewer
import xyz.angm.gamelin.windows.DebuggerWindow
import xyz.angm.gamelin.windows.GameBoyWindow
import xyz.angm.gamelin.windows.VRAMViewer
import kotlin.system.exitProcess

/** The emulator. */
open class Gamelin : ApplicationAdapter() {

    private lateinit var stage: Stage
    private lateinit var gb: GameBoy

    override fun create() {
        stage = Stage(com.badlogic.gdx.utils.viewport.ScreenViewport())
        gb = GameBoy(Gdx.files.local("opus5.gb").readBytes())
        VisUI.load()
        Gdx.input.inputProcessor = stage

        stage.addActor(DebuggerWindow(gb))
        stage.addActor(GameBoyWindow(gb))
        stage.addActor(VRAMViewer(gb))
        stage.addActor(BGMapViewer(gb))
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        if (!gb.cpu.halt) for (i in 0 until 8000) gb.advance()

        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() = exitProcess(0)
}
