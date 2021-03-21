/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 6:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.Button

/** Interface representing the user's keyboard, used for emulating the GB's joypad. */
expect object Keyboard {
    /** This callback is exeucted when the user presses a button; used for interrupts. */
    var buttonPressed: (Button) -> Unit
    /** This function should return if a button is currently pressed; used for register reads. */
    fun isPressed(btn: Button): Boolean
}