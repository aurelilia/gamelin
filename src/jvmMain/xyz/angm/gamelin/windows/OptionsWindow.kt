/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 4:19 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSlider
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import ktx.actors.onChange
import ktx.collections.*
import ktx.scene2d.defaultStyle
import ktx.scene2d.vis.*
import xyz.angm.gamelin.config
import xyz.angm.gamelin.configuration
import xyz.angm.gamelin.saveConfiguration

class OptionsWindow : Window("Options") {

    private val pane = TabbedPane()
    private val container = VisTable()

    init {
        setSize(500f, 300f)
        isResizable = true
        pane.addListener(object: TabbedPaneAdapter() {
            override fun switchedTab(tab: Tab) {
                container.clearChildren()
                container.add(tab.contentTable).expand().fill()
            }
        })

        add(pane.table).expandX().fillX().row()
        add(container).expand().fill()

        tab("System") {
            checkBox(
                "Prefer GameBoy Color Mode",
                "Run games that support both GB and GBC in GBC mode.",
                { configuration.preferCGB }, { configuration.preferCGB = it}
            )

            selectBox(
                "Fast-forward speed multiplier", arrayOf("2x", "3x", "4x", "6x", "8x"),
                { "${config.fastForwardSpeed + 1}x" },
                { config.fastForwardSpeed = it[0] - '1' }
            )
        }

        tab("Audio") {
            slider("Volume", 0f, 1f, 0.01f, { config.volume }, { config.volume = it })
            slider("Volume while fast-forwarding", 0f, 1f, 0.01f, { config.fastForwardVolume }, { config.fastForwardVolume = it })
        }
    }

    override fun close() {
        super.close()
        saveConfiguration()
    }

    private inline fun tab(name: String, init: KVisTable.() -> Unit) {
        pane.add(OptTab(name).apply { init(table) })
    }

    private inline fun KVisTable.checkBox(
        text: String,
        tooltip: String,
        get: () -> Boolean,
        crossinline set: (Boolean) -> Unit
    ): KVisCheckBox {
        visLabel(text) { it.uniform() }
        return visCheckBox("") {
            visTooltip(visLabel(tooltip))
            isChecked = get()
            onChange { set(isChecked) }
            it.colspan(2).row()
        }
    }

    private inline fun KVisTable.selectBox(
        text: String,
        items: Array<String>,
        get: () -> String,
        crossinline set: (String) -> Unit
    ): KVisSelectBox<String> {
        visLabel(text) { it.uniform() }
        val box = KVisSelectBox<String>(defaultStyle)
        box.items = GdxArray(items)
        box.selected = get()
        box.onChange { set(selected) }
        add(box).uniform().row()
        return box
    }

    private inline fun KVisTable.slider(
        text: String,
        min: Float,
        max: Float,
        step: Float,
        get: () -> Float,
        crossinline set: (Float) -> Unit
    ): VisSlider {
        visLabel(text) { it.uniform() }
        return visSlider(min, max, step) {
            value = get()
            val tooltip = VisLabel(value.toString())
            visTooltip(tooltip)
            onChange {
                set(value)
                tooltip.setText(value.toString())
            }
            it.uniform().row()
        }
    }

    private class OptTab(val title: String, val table: KVisTable = KVisTable(true)) : Tab(false, false) {
        override fun getContentTable() = table
        override fun getTabTitle() = title
    }
}