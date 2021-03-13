/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 4:54 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onClick
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.verticalGroup
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visScrollPane
import ktx.scene2d.vis.visTextButton
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.Flag
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.InstSet
import xyz.angm.gamelin.system.Reg

class DebuggerWindow(private val gb: GameBoy) : VisWindow("Debugger") {

    private val tab = KVisTable(true)
    private var active = true

    init {
        add(tab).pad(10f)
        refresh()
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (active) refresh()
        active = !gb.cpu.halt
    }

    private fun refresh() {
        tab.clearChildren()
        val next = gb.getNextInst()
        tab.run {
            defaults().left().pad(0f)

            visLabel("Next Instruction: ${next.name} (size: ${next.size})    PC: ${gb.cpu.pc.hex16()}")
            row()

            visScrollPane {
                actor = scene2d.verticalGroup {
                    left()
                    var pc = gb.cpu.pc.int() - 8
                    for (i in 0..50) {
                        val inst = InstSet.instOf(gb.read(pc), gb.read(pc + 1))
                        if (inst == null) pc += 1
                        else {
                            val label = visLabel("${pc.hex16()} ${inst.name} (${gb.read16(pc + 1).hex16()})")
                            if (pc == gb.cpu.pc.int()) label.color = Color.OLIVE
                            pc += inst.size
                        }
                    }
                }
                it.height(300f).width(500f).expandX()
            }

            row()
            visScrollPane {
                actor = scene2d.verticalGroup {
                    left()
                    for (row in 0x8000 until 0xA000 step 16) {
                        var out = "VRAM:${row.hex16()} "
                        for (by in 0 until 16) {
                            out += "${gb.read(row + by).hex8()} "
                        }
                        visLabel(out)
                    }
                    var out = "FF40: ${gb.read(0xFF40).hex8()} "
                    visLabel(out)
                }
                it.height(300f).width(500f).expandX()
            }


            row()
            horizontalGroup {
                visLabel("Flags: ")
                for (flag in Flag.values()) visLabel("  $flag = ${gb.cpu.flagVal(flag)}  ")
            }

            row()
            horizontalGroup {
                visLabel("Regs: ")
                for (reg in Reg.values()) visLabel("  $reg = ${gb.read(reg).hex8()}  ")
            }
            visLabel("SP: 0x${gb.readSP().hex16()}")
            row()

            visTextButton("Advance") {
                onClick {
                    gb.advance(force = true)
                    refresh()
                }
            }
        }
    }
}