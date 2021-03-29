/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 7:17 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows.options

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.alpha
import ktx.actors.onClick
import ktx.collections.*
import xyz.angm.gamelin.Gamelin
import xyz.angm.gamelin.config
import xyz.angm.gamelin.system.io.Button
import xyz.angm.gamelin.windows.leftLabel

private fun keyCodeToString(code: Int) = if (code < 0) "None" else Input.Keys.toString(code)
private fun buttonToString(code: Int) = if (code < 0) "None" else "Button $code"

abstract class InputPane<C>(private val keyboard: Boolean) : VisTable(true) {

    protected var current: C? = null
    private val buttons = GdxArray<VisTextButton>()
    private var button = 0
    private val controllerListener = object : ControllerAdapter() {
        override fun buttonDown(controller: Controller?, buttonIndex: Int): Boolean {
            registerButton(buttonIndex)
            resetBtns()
            return true
        }

        override fun axisMoved(controller: Controller?, axisIndex: Int, value: Float): Boolean {
            return if (value > 0.8f || value < -0.8f) {
                registerAxis(axisIndex)
                resetBtns()
                true
            } else false
        }
    }

    fun onShow() = resetBtns()

    fun onHide() {
        current = null
        Controllers.removeListener(controllerListener)
    }

    private fun resetBtns() {
        clearListeners()
        Controllers.removeListener(controllerListener)
        buttons.clear()
        reset()
        createButtons()
    }

    abstract fun createButtons()

    protected fun createButton(label: String, btnText: String, clickedValue: C) {
        leftLabel(label)

        val btn = VisTextButton(btnText)
        add(btn).width(75f).height(35f).uniform()

        val btnIdx = buttons.size
        buttons.add(btn)
        btn.onClick { clicked(btnIdx, clickedValue) }

        if (buttons.size % 2 == 0) row()
        else add().width(50f)
    }

    private fun clicked(btn: Int, value: C) {
        resetBtns()
        buttons[btn].setText("...")
        buttons[btn].alpha = 0.7f
        current = value
        button = btn
        stage.keyboardFocus = this

        if (keyboard) {
            addListener { event ->
                if (event is InputEvent && event.type === InputEvent.Type.keyDown && current != null) {
                    registerKey(event.keyCode)
                    resetBtns()
                    true
                } else false
            }
        } else Controllers.addListener(controllerListener)
    }

    protected open fun registerKey(key: Int) {}
    protected open fun registerButton(button: Int) {}
    protected open fun registerAxis(axis: Int) {}
}

class KeyboardInputPane : InputPane<Int>(keyboard = true) {
    override fun createButtons() {
        for (btn in Button.values()) {
            createButton(btn.name, keyCodeToString(config.keymap[btn.ordinal]), btn.ordinal)
        }
    }

    override fun registerKey(key: Int) {
        config.keymap[current ?: 0] = if (key == Input.Keys.ESCAPE) -1 else key
    }
}

class KeyboardHotkeyPane(private val emu: Gamelin) : InputPane<String>(keyboard = true) {
    override fun createButtons() {
        for (bind in config.hotkeys) {
            val name = bind.key
            val key = bind.value
            createButton(name, keyCodeToString(key), name)
        }
    }

    override fun registerKey(key: Int) {
        config.hotkeys[current ?: return] = if (key == Input.Keys.ESCAPE) -1 else key
        emu.hotkeyHandler.update()
    }
}

class ControllerInputPane : InputPane<Int>(keyboard = false) {
    override fun createButtons() {
        for (btn in Button.values()) {
            createButton(btn.name, buttonToString(config.buttonMap[btn.ordinal]), btn.ordinal)
        }
        createButton("Left/Right Axis", "Axis ${config.axisMap[0]}", 100)
        createButton("Up/Down Axis", "Axis ${config.axisMap[1]}", 101)
    }

    override fun registerButton(button: Int) {
        if (current!! > config.buttonMap.size) return
        config.buttonMap[current ?: 0] = button
    }

    override fun registerAxis(axis: Int) {
        if (current!! < 100) return
        config.axisMap[(current ?: 0) - 100] = axis
    }
}

class ControllerHotkeyPane(private val emu: Gamelin) : InputPane<String>(keyboard = false) {
    override fun createButtons() {
        for (bind in config.hotkeyButtons) {
            val name = bind.key
            val button = bind.value
            createButton(name, buttonToString(button), name)
        }
    }

    override fun registerButton(button: Int) {
        config.hotkeyButtons[current ?: return] = button
        emu.hotkeyHandler.update()
    }
}