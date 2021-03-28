/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 4:37 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import kotlinx.browser.window
import xyz.angm.gamelin.system.io.Button

private val btnToKey = arrayOf(90, 88, 13, 32, 39, 37, 38, 40)

actual object Keyboard {

    private val pressed = BooleanArray(btnToKey.size)
    private val keyToIdx = HashMap<Int, Int>()

    init {
        for ((i, key) in btnToKey.withIndex()) {
            keyToIdx[key] = i
        }

        window.onkeydown = {
            val btn = keyToIdx[it.keyCode]
            if (btn != null) {
                pressed[btn] = true
                buttonPressed(Button.values()[btn])
            }
        }
        window.onkeyup = {
            val btn = keyToIdx[it.keyCode]
            if (btn != null) pressed[btn] = false
        }
    }

    actual var buttonPressed: (Button) -> Unit = {}

    actual fun isPressed(btn: Button) = pressed[btn.ordinal]
}