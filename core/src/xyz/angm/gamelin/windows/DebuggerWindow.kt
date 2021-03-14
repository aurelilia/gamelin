/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 4:33 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onClick
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.*

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
        active = !gb.debugger.emuHalt
    }

    private fun refresh() {
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
                    var pc = gb.cpu.pc.int() - 8
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
                defaults().left().pad(0f).padLeft(2f).expandX()
                visLabel("DRegs: ") { it.row() }
                for (reg in arrayOf(DReg.AF, DReg.BC, DReg.DE, DReg.HL)) visLabel("$reg = ${gb.read16(reg).hex16()}") { it.row() }
                visLabel("SP = ${gb.readSP().hex16()}") { it.row() }
                visLabel("PC = ${gb.cpu.pc.hex16()}") { it.row() }
                it.width(200f)
            }

            row()
            visScrollPane {
                actor = scene2d.visTable {
                    defaults().left().pad(0f).padLeft(2f).expandX()
                    val addRow = { row: Int ->
                        var out = "VRAM:${row.hex16()} "
                        for (by in 0 until 16) {
                            out += "${gb.read(row + by).hex8()} "
                        }
                        visLabel(out)
                        row()
                    }

                    for (row in 0x8180 until 0xA000 step 16) addRow(row)
                    addRow(0xFF40)
                }
                setScrollingDisabled(true, false)
                it.height(300f).width(750f).expandX().colspan(2)
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
            row()

            visTable {
                it.colspan(2)
                defaults().pad(5f).padRight(15f)
                visTextButton("Advance") {
                    onClick {
                        gb.advance(force = true)
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