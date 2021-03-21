/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 3:38 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import xyz.angm.gamelin.desktopConfiguration
import xyz.angm.gamelin.system.io.Button

actual object Keyboard : InputAdapter() {

    actual var buttonPressed: (Button) -> Unit = {}
    private val keyToBtn = IntMap<Button>()

    init {
        for (i in desktopConfiguration.keymap.indices) {
            keyToBtn[desktopConfiguration.keymap[i]] = Button.values()[i]
        }
    }

    actual fun isPressed(btn: Button): Boolean {
        val key = desktopConfiguration.keymap[btn.ordinal]
        return Gdx.input.isKeyPressed(key)
    }

    override fun keyDown(keycode: Int): Boolean {
        buttonPressed(keyToBtn[keycode] ?: return false)
        return false
    }
}