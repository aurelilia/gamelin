/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/27/21, 7:08 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import xyz.angm.gamelin.config
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.system.io.Button

/** Simple keyboard impl using libGDX input, will never
 * consider events handled and always allow them to drop to the next handler. */
actual object Keyboard : InputAdapter(), ControllerListener {

    actual var buttonPressed: (Button) -> Unit = {}
    private val keyToBtn = IntMap<Button>()
    private val controllerToBtn = IntMap<Button>()

    init {
        for (i in config.keymap.indices) {
            keyToBtn[config.keymap[i]] = Button.values()[i]
            controllerToBtn[config.buttonMap[i]] = Button.values()[i]
        }
    }

    actual fun isPressed(btn: Button): Boolean {
        val key = config.keymap[btn.ordinal]
        return Gdx.input.isKeyPressed(key) || Controllers.getCurrent()?.getButton(config.buttonMap[btn.ordinal]) == true || (btn.ordinal > 3 && axisActive(btn))
    }

    private fun axisActive(btn: Button): Boolean {
        val axis = config.axisMap[(btn.ordinal - 4) / 2]
        val value = Controllers.getCurrent()?.getAxis(axis) ?: 0f
        return if (btn == Button.Left || btn == Button.Up) value < -0.7f else value > 0.7f
    }

    override fun keyDown(keycode: Int): Boolean {
        buttonPressed(keyToBtn[keycode] ?: return false)
        return false
    }

    override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
        if (buttonCode == config.fastForwardButton) gb.mmu.sound.output.skip = config.fastForwardSpeed
        buttonPressed(controllerToBtn[buttonCode] ?: return false)
        return false
    }

    override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
        if (buttonCode == config.fastForwardButton) gb.mmu.sound.output.skip = 0
        return false
    }

    override fun axisMoved(controller: Controller?, axisCode: Int, value: Float) = false
    override fun connected(controller: Controller?) {}
    override fun disconnected(controller: Controller?) {}
}