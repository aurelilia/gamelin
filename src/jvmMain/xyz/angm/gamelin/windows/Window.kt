/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 5:55 PM.
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

    override fun addCloseButton() {
        super.addCloseButton()
        // Add a tiny bit of top padding to the close button. It covers part of the window
        // border when using the tinted skin otherwise
        val cells = titleTable.cells
        val cell = cells[cells.size - 1]
        cell.padTop(1f)
    }
}