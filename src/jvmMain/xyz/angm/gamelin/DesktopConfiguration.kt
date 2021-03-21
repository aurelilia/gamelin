/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:37 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Input

val desktopConfiguration = loadDesktopConfiguration()

/** Additional configuration only applicable to desktop. */
class DesktopConfiguration {
    val keymap = arrayOf(Input.Keys.Z, Input.Keys.X, Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.RIGHT, Input.Keys.LEFT, Input.Keys.UP, Input.Keys.DOWN)
}