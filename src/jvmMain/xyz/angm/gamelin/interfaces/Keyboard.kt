/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 3:30 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import xyz.angm.gamelin.config
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.system.io.Button

/** Simple keyboard impl using libGDX input, will never
 * consider events handled and always allow them to drop to the next handler. */
actual object Keyboard : InputAdapter() {

    actual var buttonPressed: (Button) -> Unit = {}
    private val keyToBtn = IntMap<Button>()

    init {
        for (i in config.keymap.indices) {
            keyToBtn[config.keymap[i]] = Button.values()[i]
        }
    }

    actual fun isPressed(btn: Button): Boolean {
        val key = config.keymap[btn.ordinal]
        return Gdx.input.isKeyPressed(key)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == config.fastForwardKey) gb.mmu.sound.output.skip = config.fastForwardSpeed
        buttonPressed(keyToBtn[keycode] ?: return false)
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == config.fastForwardKey) gb.mmu.sound.output.skip = 0
        return super.keyUp(keycode)
    }
}