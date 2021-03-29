/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 2:14 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import ktx.actors.*
import ktx.collections.*
import ktx.scene2d.defaultStyle
import ktx.scene2d.vis.*
import xyz.angm.gamelin.*
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.interfaces.SaveState
import xyz.angm.gamelin.interfaces.device
import xyz.angm.gamelin.system.io.Button

class OptionsWindow(private val emu: Gamelin) : Window("Options") {

    private val pane = TabbedPane()
    private val container = VisTable()

    init {
        setSize(700f, 450f)
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
                "Gamelin UI Theme", UiSkin.values().map { it.toString() }.toTypedArray(),
                { config.skin.toString() },
                {
                    config.skin = UiSkin.valueOf(it)
                    emu.toast("Please restart the emulator to apply the theme.")
                }
            )

            checkBox(
                "Confirm resets",
                "Require confirming you want to reset when hitting the reset button or hotkey.",
                { config.confirmResets }, { config.confirmResets = it }
            )

            add(Separator()).padTop(5f).padBottom(5f).fillX().colspan(2).row()

            selectBox(
                "Fast-forward speed multiplier (Hold)", arrayOf("2x", "3x", "4x", "6x", "8x"),
                { "${config.fastForwardHoldSpeed + 1}x" },
                { config.fastForwardHoldSpeed = it[0] - '1' }
            )
            selectBox(
                "Fast-forward speed multiplier (Toggle)", arrayOf("2x", "3x", "4x", "6x", "8x"),
                { "${config.fastForwardToggleSpeed + 1}x" },
                { config.fastForwardToggleSpeed = it[0] - '1' }
            )
            checkBox(
                "Enable rewinding",
                "Allow rewinding a game's last few seconds while holding a key.",
                { config.enableRewind }, {
                    config.enableRewind = it
                    SaveState.recreateBuffers()
                }
            )

            val label = VisLabel("Rewind buffer memory usage: ${"%.2f".format(SaveState.bufferSizeInMb)}MB")
            visSpinner(
                "Rewind buffer size in seconds",
                "How long you can rewind, in seconds. Will affect memory usage.",
                IntSpinnerModel(config.rewindBufferSec, 1, 60)
            ) {
                config.rewindBufferSec = it
                SaveState.recreateBuffers()
                label.setText("Rewind buffer memory usage: ${"%.2f".format(SaveState.bufferSizeInMb)}MB")
            }
            add(label).colspan(2)
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
                    emu.reloadGameWindow()
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
            slider("Volume", 0f, 1f, 0.01f, { config.volume }, {
                config.volume = it
                device.setVolume(it)
            })
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
                Keyboard.refresh()
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
            }

            addListener { event ->
                if (event is InputEvent && event.type === InputEvent.Type.keyDown && current != null) {
                    resetCurrent(button, event.keyCode)
                    true
                } else false
            }
        }

        tab("Hotkeys") {
            var current: String? = null
            var button: VisTextButton? = null

            fun resetCurrent(btn: VisTextButton?, key: Int) {
                if (current == null || btn == null) return
                if (key != -1) config.hotkeys[current!!] = if (key == Input.Keys.ESCAPE) -1 else key
                val cur = config.hotkeys[current!!]!!
                btn.setText(if (cur == -1) "None" else Input.Keys.toString(cur))
                btn.alpha = 1f
                current = null
                button = null
                emu.hotkeyHandler.update()
                emu.recreateMenus()
            }

            for ((i, bind) in config.hotkeys.withIndex()) {
                val name = bind.key
                val key = bind.value
                val keyName = if (key == -1) "None" else Input.Keys.toString(key)
                leftLabel(name)

                visTextButton(keyName) {
                    onClick {
                        setText("...")
                        alpha = 0.7f
                        resetCurrent(button, -1)
                        current = name
                        button = this@visTextButton
                        stage.keyboardFocus = this@tab
                    }
                    it.width(75f).height(35f).uniform()
                }

                if (i % 2 == 1) row()
                else add().width(50f)
            }

            addListener { event ->
                if (event is InputEvent && event.type === InputEvent.Type.keyDown && current != null) {
                    resetCurrent(button, event.keyCode)
                    true
                } else false
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
        box.setAlignment(Align.center)
        box.list.setAlignment(Align.center)
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

    private inline fun KVisTable.visSpinner(
        text: String,
        tooltip: String,
        model: IntSpinnerModel,
        crossinline set: (Int) -> Unit
    ): KSpinner {
        leftLabel(text)
        return spinner("", model) {
            visTooltip(visLabel(tooltip))
            onChange { set(model.value) }
            it.row()
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