/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 10:25 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows.debugger

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onClick
import ktx.collections.*
import ktx.scene2d.horizontalGroup
import ktx.scene2d.scene2d
import ktx.scene2d.vis.*
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.windows.DelayedUpdateWindow

class MemoryViewer : DelayedUpdateWindow("Memory Viewer", 0.5f) {

    private val labels = GdxArray<VisLabel>()

    init {
        add(scene2d.visScrollPane {
            actor = scene2d.visTable {
                defaults().left().pad(0f).padLeft(3f).expandX()
                val addRow = { name: String, row: Int ->
                    labels.add(visLabel(rowText(name, row), "monospace"))
                    row()
                }

                for (row in 0x0000 until 0x8000 step 16) addRow(" ROM", row)
                for (row in 0x8000 until 0xA000 step 16) addRow("VRAM", row)
                for (row in 0xA000 until 0xC000 step 16) addRow("CRAM", row)
                for (row in 0xC000 until 0xE000 step 16) addRow("WRAM", row)
                for (row in 0xE000 until 0xFE00 step 16) addRow("ECHO", row)
                for (row in 0xFE00 until 0xFEA0 step 16) addRow(" OAM", row)
                for (row in 0xFEA0 until 0xFF00 step 16) addRow("----", row)
                for (row in 0xFF00 until 0xFF80 step 16) addRow("MMIO", row)
                for (row in 0xFF80 until 0xFFFF step 16) addRow("HRAM", row)
            }
            setScrollingDisabled(true, false)
            fadeScrollBars = false
        }).pad(5f).height(400f).width(867f)
        pack()
    }

    override fun refresh() {
        for ((i, row) in labels.withIndex()) {
            row.setText(rowText(row.text.substring(0, 4), i * 16))
        }
    }

    private fun rowText(name: String, row: Int): String {
        var out = "$name:${row.hex16()}  "
        for (by in 0 until 16) {
            out += "${gb.read(row + by).hex8()} "
        }
        return out
    }
}