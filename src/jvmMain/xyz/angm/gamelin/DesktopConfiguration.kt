/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 7:49 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import xyz.angm.gamelin.system.io.Button

val config = loadDesktopConfiguration()

/** Additional configuration only applicable to desktop.
 * @property keymap The keys mapping to the GB joypad. Order is same as [Button].
 * @property buttonMap Same as [keymap] but for controller buttons
 * @property axisMap Map of controller axes for the dpad, first is horizontal, second is vertical
 * @property hotkeys Map of hotkeys to their mapped key.
 * @property fastForwardButton Controller button to be held to fast-forward.
 * @property fastForwardSpeed Fast-forward speed - 1. (1 = 2x, 2 = 3x, ...)
 * @property volume Volume of audio output
 * @property fastForwardVolume Volume while fast-forwarding
 * @property gbScale Scale of the GameBoy window
 * @property hqxLevel Which type of hqx scaling to apply. Values 1-4; 1 is linear scaling.
 * @property confirmResets If pressing the reset button or hotkey requires confirmation
 * @property lastOpened A list of last opened files/ROMs (absolute path). Last element is most recently opened. */
class DesktopConfiguration {
    val keymap = arrayOf(Input.Keys.Z, Input.Keys.X, Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.RIGHT, Input.Keys.LEFT, Input.Keys.UP, Input.Keys.DOWN)
    val buttonMap = arrayOf(0, 1, 9, 8, 14, 13, 11, 12)
    val axisMap = arrayOf(0, 1)

    val hotkeys = OrderedMap<String, Int>()
    var fastForwardButton = 3
    var fastForwardSpeed = 3

    var volume = 0.5f
    var fastForwardVolume = 0.3f

    var gbScale = 4
    var hqxLevel = 1
    var skin = UiSkin.Tinted

    var confirmResets = true

    val lastOpened = OrderedSet<String>()
}

enum class UiSkin(val path: String?) {
    VisUI(null), Tinted("skin/tinted.json")
}
