/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/23/21, 11:38 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onClick
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.cpu.DReg
import xyz.angm.gamelin.system.cpu.Flag
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.system.cpu.Reg
import xyz.angm.gamelin.system.io.MMU

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
        active = !gb.debugger.emuHalt
    }

    override fun refresh() {
        tab.clearChildren()
        val next = gb.getNextInst()
        tab.run {
            defaults().left().pad(0f)

            visLabel("Next Instruction: ${next.name} (size: ${next.size})    PC: ${gb.cpu.pc.hex16()}")
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
                            val label = visLabel("${pc.hex16()} ${inst.name} (${gb.read16(pc + 1).hex16()})")
                            if (pc == gb.cpu.pc.int()) label.color = Color.OLIVE
                            pc += inst.size
                            row()
                        }
                    }
                }
                setScrollingDisabled(true, false)
                it.height(300f).width(500f).expandX()
            }
            visTable {
                visScrollPane {
                    actor = scene2d.visTable {
                        defaults().left().pad(0f).padLeft(2f).expandX()
                        for (i in 0 until 10 step 2) {
                            val location = gb.cpu.sp - i
                            visLabel("${(location and 0xFFFF).hex16()} = ${gb.read16(location).hex16()}") { it.row() }
                        }
                    }
                    setScrollingDisabled(true, false)
                    it.height(300f).width(200f)
                }

                visTable {
                    defaults().left().pad(0f).padLeft(2f).expandX()
                    visLabel("DRegs: ") { it.row() }
                    for (reg in arrayOf(DReg.AF, DReg.BC, DReg.DE, DReg.HL)) visLabel("$reg = ${gb.read16(reg).hex16()}") { it.row() }
                    visLabel("SP = ${gb.readSP().hex16()}") { it.row() }
                    visLabel("PC = ${gb.cpu.pc.hex16()}") { it.row() }
                    it.width(200f)
                }
            }

            row()
            visScrollPane {
                actor = scene2d.visTable {
                    defaults().left().pad(0f).padLeft(2f).expandX()
                    val addRow = { name: String, row: Int ->
                        var out = "$name:${row.hex16()} "
                        for (by in 0 until 16) {
                            out += "${gb.read(row + by).hex8()} "
                        }
                        visLabel(out)
                        row()
                    }

                    for (row in 0xFE00 until 0xFEA0 step 16) addRow("OAM ", row)
                    for (row in 0xFF00 until 0xFF80 step 16) addRow("MMIO", row)
                    for (row in 0xFF80 until 0xFFFF step 16) addRow("HRAM", row)
                }
                setScrollingDisabled(true, false)
                it.height(300f).width(750f).expandX().colspan(2)
            }

            row()
            horizontalGroup {
                visLabel("Flags: ")
                for (flag in Flag.values()) visLabel("  $flag = ${gb.cpu.flagVal(flag)}  ")
                visLabel("  IME = ${if (gb.cpu.ime) "enabled" else "disabled"}  ")
                visLabel("  HALT = ${if (gb.cpu.halt) "yes" else "no"}  ")
            }

            row()
            horizontalGroup {
                visLabel("Regs: ")
                for (reg in Reg.values()) visLabel("  $reg = ${gb.read(reg).hex8()}  ")
            }

            row()
            horizontalGroup {
                visLabel("Timer: ")
                visLabel("  DIV = ${gb.read(MMU.DIV).hex8()}  ")
                visLabel("  TIMA = ${gb.read(MMU.TIMA).hex8()}  ")
                visLabel("  TMA = ${gb.read(MMU.TMA).hex8()}  ")
                visLabel("  TAC = ${gb.read(MMU.TAC).hex8()}  ")
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

                var field: VisTextField? = null
                visCheckBox("PC BP: 0x") {
                    isChecked = gb.debugger.pcBreakEnable
                    onClick {
                        try {
                            gb.debugger.pcBreak = Integer.parseUnsignedInt(field!!.text, 16)
                            gb.debugger.pcBreakEnable = !gb.debugger.pcBreakEnable
                            field!!.isDisabled = gb.debugger.pcBreakEnable
                        } catch (e: Exception) {
                            gb.debugger.pcBreakEnable = false
                            field!!.isDisabled = false
                        }
                    }
                    it.pad(0f)
                }
                field = visTextField {
                    text = gb.debugger.pcBreak.toString(16).toUpperCase()
                    isDisabled = gb.debugger.pcBreakEnable
                }

                var field2: VisTextField? = null
                visCheckBox("Write BP: 0x") {
                    isChecked = gb.debugger.writeBreakEnable
                    onClick {
                        try {
                            gb.debugger.writeBreak = Integer.parseUnsignedInt(field2!!.text, 16)
                            gb.debugger.writeBreakEnable = !gb.debugger.writeBreakEnable
                            field2!!.isDisabled = gb.debugger.writeBreakEnable
                        } catch (e: Exception) {
                            gb.debugger.writeBreakEnable = false
                            field2!!.isDisabled = false
                        }
                    }
                    it.pad(0f)
                }
                field2 = visTextField {
                    text = gb.debugger.writeBreak.toString(16).toUpperCase()
                    isDisabled = gb.debugger.writeBreakEnable
                }

                visCheckBox("CPU Logging") {
                    isChecked = gb.debugger.loggingEnable
                    onClick { gb.debugger.loggingEnable = !gb.debugger.loggingEnable }
                }
            }
        }
    }
}