/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 8:34 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onClick
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTextButton
import xyz.angm.gamelin.system.Flag
import xyz.angm.gamelin.system.GameBoy

class DebuggerWindow(private val gb: GameBoy) : VisWindow("Debugger") {

    private val tab = KVisTable(true)

    init {
        add(tab).pad(10f)
        refresh()
        pack()
    }

    private fun refresh() {
        tab.clearChildren()
        val next = gb.getNextInst()
        tab.run {
            visLabel("Next Instruction: ${next.name} (size: ${next.size})")
            row()
            visLabel("Memory at PC (0x${gb.cpu.pc.toString(16)}): ")
            for (by in 0 until 5) visLabel("0x${gb.read(gb.cpu.pc + by).toString(16)}  ")
            row()
            visLabel("Stack area: ")
            for (mem in 0xFFEF until 0xFFFF) visLabel("0x${gb.read(mem).toString(16)}  ")
            row()
            visLabel("Flags: ")
            row()
            for (flag in Flag.values()) {
                visLabel("$flag: ${gb.cpu.flag(flag)}")
                row()
            }
            visTextButton("Advance") {
                onClick {
                    gb.advance()
                    refresh()
                }
            }
        }
    }
}