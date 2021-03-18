/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:38 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.Button

expect object Keyboard {
    var buttonPressed: (Button) -> Unit
    fun isPressed(btn: Button): Boolean
}