/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 11:46 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows.debugger

import com.badlogic.gdx.graphics.Color
import ktx.actors.onClick
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import xyz.angm.gamelin.*
import xyz.angm.gamelin.system.cpu.DReg
import xyz.angm.gamelin.system.cpu.Flag
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.system.io.MMU
import xyz.angm.gamelin.windows.DelayedUpdateWindow

/** Debugger window that shows a bunch of current system state. */
class DebuggerWindow : DelayedUpdateWindow("Debugger", 0.5f) {

    private val tab = KVisTable(true)
    private var active = true

    init {
        add(tab).pad(10f)
        refresh()
        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (!active) sinceUpdate = 0f
        val wasActive = active
        active = !gb.debugger.emuHalt
        if (!active && wasActive) refresh()
    }

    override fun refresh() {
        tab.clearChildren()
        val next = gb.getNextInst()
        tab.run {
            defaults().left().pad(0f)

            horizontalGroup {
                visLabel("Next Instruction:  ")
                visLabel("${next.name}, size ${next.size}", "monospace")
            }
            visCheckBox("Halted") {
                isChecked = gb.debugger.emuHalt
                onClick {
                    gb.debugger.emuHalt = !gb.debugger.emuHalt
                    refresh()
                }
            }
            row()

            visScrollPane {
                actor = scene2d.visTable {
                    defaults().left().pad(0f).padLeft(2f).expandX()
                    var pc = gb.cpu.pc.int()
                    for (i in 0..50) {
                        val inst = InstSet.instOf(gb.read(pc), gb.read(pc + 1))
                        if (inst == null) pc += 1
                        else {
                            val label = visLabel("${pc.hex16()} ${inst.name} (${gb.read16(pc + 1).hex16()})", "monospace")
                            if (pc == gb.cpu.pc.int()) label.color = Color.OLIVE
                            pc += inst.size
                            row()
                        }
                    }
                }
                setScrollingDisabled(true, false)
                it.height(300f).width(300f).expandX()
            }
            visTable {
                visScrollPane {
                    actor = scene2d.visTable {
                        defaults().left().pad(0f).padLeft(2f).expandX()
                        visLabel("Stack: ") { it.row() }
                        for (i in 0 until 23 step 2) {
                            val location = gb.cpu.sp - i
                            visLabel("${(location and 0xFFFF).hex16()} = ${gb.read16(location).hex16()}", "monospace") {
                                if (i == 0) color = Color.OLIVE
                                it.row()
                            }
                        }
                    }
                    setScrollingDisabled(true, false)
                    it.height(300f).width(200f).padRight(5f)
                }

                visTable {
                    defaults().left().pad(0f).padLeft(2f).expandX()
                    visLabel("Registers: ") { it.row() }
                    for (reg in arrayOf(DReg.AF, DReg.BC, DReg.DE, DReg.HL)) visLabel("$reg = ${gb.read16(reg).hex16()}", "monospace") { it.row() }
                    visLabel("SP = ${gb.readSP().hex16()}", "monospace") { it.row() }
                    visLabel("PC = ${gb.cpu.pc.hex16()}", "monospace") { it.row() }
                    it.width(200f)
                }
                it.left()
            }

            row()
            horizontalGroup {
                visLabel("Flags: ")
                for (flag in Flag.values()) visLabel("  $flag = ${gb.cpu.flagVal(flag)}  ")
                visLabel("  IME = ${if (gb.cpu.ime) "enabled" else "disabled"}  ")
                visLabel("  HALT = ${if (gb.cpu.halt) "yes" else "no"}  ")
                visLabel("  CGB 2x = ${if (gb.tSpeedMultiplier == 2) "yes" else "no"}  ")
                it.colspan(2)
            }

            row()
            visTable {
                it.colspan(2)
                defaults().pad(5f).padRight(15f)
                visTextButton("Advance") {
                    onClick {
                        gb.advance()
                        refresh()
                    }
                }

                visTextButton("Force Update") { onClick { refresh() } }

                visTextButton("Memory Viewer") { onClick { emu.toggleWindow("Memory Viewer") { MemoryViewer() } } }
                visTextButton("Breakpoints") { onClick { emu.toggleWindow("Breakpoints") { BreakpointWindow() } } }

                visCheckBox("CPU Logging") {
                    isChecked = gb.debugger.loggingEnable
                    onClick { gb.debugger.loggingEnable = !gb.debugger.loggingEnable }
                }
            }
        }
    }
}