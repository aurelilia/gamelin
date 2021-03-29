/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 6:26 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.alpha
import ktx.actors.onClick
import ktx.collections.*
import xyz.angm.gamelin.config
import xyz.angm.gamelin.system.io.Button

private fun keyCodeToString(code: Int) = if (code < 0) "None" else Input.Keys.toString(code)

abstract class InputPane<C> : VisTable(true) {

    protected var current: C? = null
    private val buttons = GdxArray<VisTextButton>()
    private var button = 0

    init {
        resetBtns()
    }

    private fun resetBtns() {
        clearListeners()
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
        addListener { event ->
            if (event is InputEvent && event.type === InputEvent.Type.keyDown && current != null) {
                register(event.keyCode)
                resetBtns()
                true
            } else false
        }
    }

    abstract fun register(key: Int)
}

class KeyboardInputPane : InputPane<Int>() {
    override fun createButtons() {
        for (btn in Button.values()) {
            createButton(btn.name, keyCodeToString(config.keymap[btn.ordinal]), btn.ordinal)
        }
    }

    override fun register(key: Int) {
        config.keymap[current ?: 0] = if (key == Input.Keys.ESCAPE) -1 else key
    }
}

class KeyboardHotkeyPane : InputPane<String>() {
    override fun createButtons() {
        for (bind in config.hotkeys) {
            val name = bind.key
            val key = bind.value
            createButton(name, keyCodeToString(key), name)
        }
    }

    override fun register(key: Int) {
        config.hotkeys[current ?: return] = if (key == Input.Keys.ESCAPE) -1 else key
    }
}