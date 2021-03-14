/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 8:54 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.VisUI
import ktx.assets.file
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
        gb = GameBoy(file("roms/ttt.gb").readBytes())
        gb.debugger.emuHalt = true
        VisUI.load()

        val multi = InputMultiplexer()
        multi.addProcessor(gb.keyboard)
        multi.addProcessor(stage)
        Gdx.input.inputProcessor = multi

        stage.addActor(DebuggerWindow(gb))
        stage.addActor(GameBoyWindow(gb))
        stage.addActor(VRAMViewer(gb))
        stage.addActor(BGMapViewer(gb))
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        gb.advanceDelta(delta)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() {
        gb.dispose()
        stage.dispose()
        exitProcess(0)
    }
}
