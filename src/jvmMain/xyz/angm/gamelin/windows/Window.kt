/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 8:49 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow

abstract class Window(name: String, closable: Boolean = true) : VisWindow(name) {

    init {
        if (closable) addCloseButton()
        this.name = name
    }

    final override fun addCloseButton() {
        super.addCloseButton()
        padCloseButton(this)
    }

    companion object {
        // Add a tiny bit of top padding to the close button. It covers part of the window
        // border when using the tinted skin otherwise
        fun padCloseButton(window: VisWindow) {
            val cells = window.titleTable.cells
            val cell = cells[cells.size - 1]
            cell.padTop(1f)
        }
    }
}

fun VisTable.leftLabel(text: String) {
    add(VisLabel(text)).uniform().left()
}
