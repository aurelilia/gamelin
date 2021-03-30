/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:36 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import xyz.angm.gamelin.config
import xyz.angm.gamelin.system.io.Button

/** Simple keyboard impl using libGDX input, will never
 * consider events handled and always allow them to drop to the next handler.
 * Also responsible for handling controller input. */
actual object Keyboard : InputProcessor by InputAdapter(), ControllerListener by ControllerAdapter() {

    private const val AXIS_THRESHOLD = 0.7f

    actual var buttonPressed: (Button) -> Unit = {}

    private val keyToBtn = IntMap<Button>()
    private val controllerToBtn = IntMap<Button>()
    private val axisToBtns = IntMap<Pair<Button, Button>>()

    private val controllerPressed = BooleanArray(8)
    private val axisPressed = BooleanArray(8)

    init {
        refresh()
    }

    fun refresh() {
        keyToBtn.clear()
        controllerToBtn.clear()
        axisToBtns.clear()

        for (i in config.keymap.indices) {
            keyToBtn[config.keymap[i]] = Button.values()[i]
            controllerToBtn[config.buttonMap[i]] = Button.values()[i]
        }
        axisToBtns[config.axisMap[0]] = Pair(Button.Left, Button.Right)
        axisToBtns[config.axisMap[1]] = Pair(Button.Up, Button.Down)
    }

    actual fun isPressed(btn: Button): Boolean {
        val key = config.keymap[btn.ordinal]
        return Gdx.input.isKeyPressed(key) || controllerPressed[btn.ordinal] || axisPressed[btn.ordinal]
    }

    override fun keyDown(keycode: Int): Boolean {
        buttonPressed(keyToBtn[keycode] ?: return false)
        return false
    }

    override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
        val button = controllerToBtn[buttonCode]
        buttonPressed(button ?: return false)
        controllerPressed[button.ordinal] = true
        return false
    }

    override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
        val button = controllerToBtn[buttonCode] ?: return false
        controllerPressed[button.ordinal] = false
        return false
    }

    override fun axisMoved(controller: Controller?, axisCode: Int, value: Float): Boolean {
        val buttons = axisToBtns[axisCode] ?: return false
        axisPressed[buttons.first.ordinal] = value < -AXIS_THRESHOLD
        axisPressed[buttons.second.ordinal] = value > AXIS_THRESHOLD
        return false
    }
}