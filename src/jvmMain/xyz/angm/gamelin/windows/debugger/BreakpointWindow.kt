/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 11:40 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows.debugger

import com.kotcrab.vis.ui.widget.VisLabel
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.collections.*
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.interfaces.Debugger
import xyz.angm.gamelin.windows.Window

class BreakpointWindow : Window("Breakpoints") {

    private val bpTable: KVisTable

    init {
        add(scene2d.visTable {
            visTable {
                defaults().left().top().pad(0f).padLeft(2f).expandX()
                bpTable = this
                add(VisLabel("Location")).expandX().fillX()
                add(VisLabel("On PC"))
                add(VisLabel("On Write"))
                add().width(50f).row()
                it.pad(5f).padTop(20f).minHeight(100f).expandX().fillX().row()
            }

            visTable {
                defaults().pad(10f)
                visLabel("0x") { it.padRight(0f) }
                val field = visTextField { it.padLeft(0f) }
                val onPC = visCheckBox("On PC")
                val onWrite = visCheckBox("On Write")
                visCheckBox("Enable BPs") {
                    onChange { gb.debugger.enableBreakpoints = isChecked }
                }
                visTextButton("Add") { onClick {
                    try {
                        val location = (Integer.parseUnsignedInt(field.text, 16) and 0xFFFF).toShort()
                        val bp = Debugger.Breakpoint(location, onPC.isChecked, onWrite.isChecked)
                        addBP(bp)
                    } catch (e: Exception) { }
                } }
            }

            setFillParent(true)
        })
        pack()
        isResizable = true
    }

    private fun addBP(bp: Debugger.Breakpoint) {
        gb.debugger.breakpoints.add(bp)
        bpTable.apply {
            val loc = visLabel(bp.location.hex16())
            val b1 = visCheckBox("") {
                isChecked = bp.onPC
                onChange { bp.onPC = isChecked }
            }
            val b2 = visCheckBox("") {
                isChecked = bp.onWrite
                onChange { bp.onWrite = isChecked }
            }
            visTextButton("Remove") {
                onClick {
                    loc.remove()
                    b1.remove()
                    b2.remove()
                    this@visTextButton.remove()
                    gb.debugger.breakpoints.remove(bp)
                }
                it.row()
            }
        }
        pack()
    }
}