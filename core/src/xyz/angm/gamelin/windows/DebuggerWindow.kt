/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 9:15 PM.
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
import ktx.scene2d.vis.*
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
    private var sinceUpdate = 0f

    init {
        add(tab).pad(10f)
        refresh()
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        sinceUpdate += delta
        if (active && sinceUpdate > 0.5f) {
            refresh()
            sinceUpdate = 0f
        }
        active = !gb.cpu.halt
    }

    private fun refresh() {
        tab.clearChildren()
        val next = gb.getNextInst()
        tab.run {
            defaults().left().pad(0f)

            visLabel("Next Instruction: ${next.name} (size: ${next.size})    PC: ${gb.cpu.pc.hex16()}")
            row()
            visCheckBox("Halted") {
                isChecked = gb.cpu.halt
                onClick {
                    gb.cpu.halt = !gb.cpu.halt
                    refresh()
                }
            }
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
                    val addRow = { row: Int ->
                        var out = "VRAM:${row.hex16()} "
                        for (by in 0 until 16) {
                            out += "${gb.read(row + by).hex8()} "
                        }
                        visLabel(out)
                    }

                    for (row in 0x8000 until 0xA000 step 16) addRow(row)
                    addRow(0xFF40)
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
            visLabel("SP: ${gb.readSP().hex16()}")
            row()

            horizontalGroup {
                visTextButton("Advance") {
                    onClick {
                        gb.advance(force = true)
                        refresh()
                    }
                }

                visTextButton("Force Update") {
                    onClick { refresh() }
                }
            }
        }
    }
}