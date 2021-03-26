/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 7:30 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Input
import xyz.angm.gamelin.system.io.Button

val config = loadDesktopConfiguration()

/** Additional configuration only applicable to desktop.
 * @property keymap The keys mapping to the GB joypad. Order is same as [Button].
 * @property fastForwardKey Key to be held to fast-forward.
 * @property fastForwardSpeed Fast-forward speed - 1. (1 = 2x, 2 = 3x, ...)
 * @property volume Volume of audio output
 * @property fastForwardVolume Volume while fast-forwarding
 * @property gbScale Scale of the GameBoy window
 * @property hqxLevel Which type of hqx scaling to apply. Values 1-4; 1 is linear scaling. */
class DesktopConfiguration {
    val keymap = arrayOf(Input.Keys.Z, Input.Keys.X, Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.RIGHT, Input.Keys.LEFT, Input.Keys.UP, Input.Keys.DOWN)

    var fastForwardKey = Input.Keys.C
    var fastForwardSpeed = 3

    var volume = 0.5f
    var fastForwardVolume = 0.3f

    var gbScale = 4
    var hqxLevel = 1
}
