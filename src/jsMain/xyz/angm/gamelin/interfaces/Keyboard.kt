/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:29 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.soywiz.korev.Key
import view
import xyz.angm.gamelin.system.io.Button

private val btnToKey = arrayOf(Key.Z, Key.X, Key.ENTER, Key.SPACE, Key.RIGHT, Key.LEFT, Key.UP, Key.DOWN)

actual object Keyboard {
    // JS doesn't implement joypad interrupts as KORGE doesn't seem to have easy support for it,
    // shouldn't affect most games anyway as it almost always goes unused.
    actual var buttonPressed: (Button) -> Unit = {}

    // Be a bit more lenient by also accepting justReleased, JS lagging can often cause dropped input
    actual fun isPressed(btn: Button) = view.keys[btnToKey[btn.ordinal]] || view.keys.justReleased(btnToKey[btn.ordinal])
}