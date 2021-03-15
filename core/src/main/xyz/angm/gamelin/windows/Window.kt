/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 3:27 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisWindow

abstract class Window(name: String, closable: Boolean = true) : VisWindow(name) {
    init {
        if (closable) addCloseButton()
        this.name = name
    }
}