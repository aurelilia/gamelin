/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:41 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.Button

actual object Keyboard {
    actual var buttonPressed: (Button) -> Unit
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun isPressed(btn: Button): Boolean {
        TODO("Not yet implemented")
    }
}