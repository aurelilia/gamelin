/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 4:21 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import xyz.angm.gamelin.system.InstSet

class InstructionSetWindow : VisWindow("Known Instruction Set") {

    init {
        var idx = 0
        for (inst in InstSet.op) {
            val text = if (inst != null) "${inst.name}\n${inst.size}b ${inst.cycles}m" else "unknown\nopcode"
            add(VisTextButton(text)).size(120f, 60f).pad(3f)
            if ((--idx % 16) == 0) row()
        }
        pack()
    }
}