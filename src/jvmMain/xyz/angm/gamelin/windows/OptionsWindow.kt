/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 9:53 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import ktx.actors.*
import ktx.collections.*
import ktx.scene2d.defaultStyle
import ktx.scene2d.vis.*
import xyz.angm.gamelin.*
import xyz.angm.gamelin.system.io.Button

class OptionsWindow(private val game: Gamelin) : Window("Options") {

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
                { configuration.preferCGB }, { configuration.preferCGB = it }
            )

            selectBox(
                "Fast-forward speed multiplier", arrayOf("2x", "3x", "4x", "6x", "8x"),
                { "${config.fastForwardSpeed + 1}x" },
                { config.fastForwardSpeed = it[0] - '1' }
            )
        }

        tab("Graphics") {
            checkBox(
                "GBC Color correction",
                "Filter colors to be closer to how they look on a real GBC screen.",
                { configuration.cgbColorCorrection }, { configuration.cgbColorCorrection = it }
            )

            selectBox(
                "GameBoy display scale", arrayOf("1x", "2x", "3x", "4x", "6x", "8x"),
                { "${config.gbScale}x" },
                {
                    config.gbScale = it[0] - '0'
                    game.reloadGameWindow()
                }
            )

            val upscalers = arrayOf("None", "hq2x", "hq3x", "hq4x")
            selectBox(
                "Upscaling method", upscalers,
                { upscalers[config.hqxLevel - 1] },
                {
                    config.hqxLevel = upscalers.indexOf(it) + 1
                    gb.mmu.ppu.renderer.hqxLevelChanged()
                }
            )
        }

        tab("Audio") {
            slider("Volume", 0f, 1f, 0.01f, { config.volume }, { config.volume = it })
            slider("Volume while fast-forwarding", 0f, 1f, 0.01f, { config.fastForwardVolume }, { config.fastForwardVolume = it })
        }

        tab("Input") {
            var current: Int? = null
            var button: VisTextButton? = null

            fun resetCurrent(btn: VisTextButton?, key: Int = config.keymap[current ?: 0]) {
                if (current == null || btn == null) return
                config.keymap[current ?: 0] = key
                btn.setText(Input.Keys.toString(key))
                btn.alpha = 1f
                current = null
                button = null
            }

            for (btn in Button.values()) {
                leftLabel(btn.name)

                val key = config.keymap[btn.ordinal]
                visTextButton(Input.Keys.toString(key)) {
                    onClick {
                        setText("...")
                        alpha = 0.7f
                        resetCurrent(button)
                        current = btn.ordinal
                        button = this@visTextButton
                        stage.keyboardFocus = this@tab
                    }
                    it.width(75f).height(35f).uniform()
                }

                if (btn.ordinal % 2 == 1) row()
                else add().width(50f)

                addListener { event ->
                    if (event is InputEvent && event.type === InputEvent.Type.keyDown && current != null) {
                        resetCurrent(button, event.keyCode)
                        true
                    } else false
                }
            }
        }

        tab("Hotkeys") {
            visLabel("Fast-forward") { it.uniform() }

            val button = visTextButton(Input.Keys.toString(config.fastForwardKey)) {
                onClick {
                    setText("...")
                    alpha = 0.7f
                    stage.keyboardFocus = this@tab
                }
                it.width(75f).height(35f).uniform()
            }

            onKeyDown(true) {
                config.fastForwardKey = it
                button.setText(Input.Keys.toString(it))
                button.alpha = 1f
            }
        }

        pane.switchTab(0)
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
        leftLabel(text)
        return visCheckBox("") {
            visTooltip(visLabel(tooltip))
            isChecked = get()
            onChange { set(isChecked) }
            it.row()
        }
    }

    private inline fun KVisTable.selectBox(
        text: String,
        items: Array<String>,
        get: () -> String,
        crossinline set: (String) -> Unit
    ): KVisSelectBox<String> {
        leftLabel(text)
        val box = KVisSelectBox<String>(defaultStyle)
        box.items = GdxArray(items)
        box.selected = get()
        box.onChange { set(selected) }
        add(box).uniform().width(75f).row()
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
        leftLabel(text)
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

    private fun KVisTable.leftLabel(text: String) {
        visLabel(text) { it.uniform().left() }
    }

    private class OptTab(val title: String, val table: KVisTable = KVisTable(true)) : Tab(false, false) {
        override fun getContentTable() = table
        override fun getTabTitle() = title
    }
}