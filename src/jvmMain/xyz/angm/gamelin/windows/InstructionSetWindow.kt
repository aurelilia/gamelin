/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:36 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisTextButton
import xyz.angm.gamelin.system.cpu.Inst

/** Table displaying the given instruction set in a 16x16 table. */
class InstructionSetWindow(name: String, set: Array<Inst?>) : Window(name) {

    init {
        var idx = 0
        for (inst in set) {
            val text = if (inst != null) "${inst.name}\n${inst.size}b ${inst.cycles + inst.preCycles}m" else "unknown\nopcode"
            val button = VisTextButton(text)
            button.color = if (inst != null) Color.OLIVE else Color.SALMON
            add(button).size(120f, 60f).pad(3f)
            if ((--idx % 16) == 0) row()
        }
        pack()
    }
}