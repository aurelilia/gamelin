/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 6:10 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.IntMap
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import com.kotcrab.vis.ui.widget.file.StreamingFileChooserListener
import ktx.collections.*
import xyz.angm.gamelin.interfaces.DesktopDebugger
import xyz.angm.gamelin.interfaces.FileSystem
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.windows.*
import kotlin.system.exitProcess

var gb = GameBoy(DesktopDebugger())

class Gamelin : ApplicationAdapter() {

    private lateinit var stage: Stage
    private val windows = HashMap<String, Window>()
    private val hotkeyHandler = HotkeyHandler()
    private lateinit var gbWindow: GameBoyWindow
    private lateinit var saveGameBtn: MenuItem

    override fun create() {
        VisUI.load()
        stage = Stage(com.badlogic.gdx.utils.viewport.ScreenViewport())
        val root = VisTable()

        val multi = InputMultiplexer()
        multi.addProcessor(Keyboard)
        multi.addProcessor(stage)
        multi.addProcessor(hotkeyHandler)
        Gdx.input.inputProcessor = multi

        gbWindow = GameBoyWindow()
        stage.addActor(gbWindow)
        stage.addActor(root)

        root.setFillParent(true)
        createMenus(root)
        root.add().expand().fill()

        Thread {
            while (!gb.disposed) {
                gb.advanceIndefinitely { Thread.sleep(16) }
            }
        }.start()
    }

    private fun createMenus(root: VisTable) {
        val menuBar = MenuBar()
        val file = Menu("File")
        val view = Menu("View")
        val options = Menu("Options")

        val chooser = createFileChooser()
        file.item("Load ROM", Input.Keys.O) {
            stage.addActor(chooser)
            chooser.fadeIn()
        }
        file.item("Reset", Input.Keys.R) { gb.reset() }
        saveGameBtn = file.item("Save Game to disk", Input.Keys.S) { gb.mmu.cart.save() }
        saveGameBtn.isDisabled = true
        file.item("Exit", Input.Keys.F4) { Gdx.app.exit() }

        fun windowItem(name: String, shortcut: Int?, create: () -> Window) {
            view.item(name, shortcut) { toggleWindow(name, create) }
        }

        windowItem("Debugger", Input.Keys.D) { DebuggerWindow() }
        windowItem("BG Map Viewer", Input.Keys.B) { BGMapViewer() }
        windowItem("VRAM Viewer", Input.Keys.V) { VRAMViewer() }
        windowItem("Cartridge Info", Input.Keys.I) { CartInfoWindow() }
        windowItem("Instruction Set", null) { InstructionSetWindow("Instruction Set", InstSet.op) }
        windowItem("Extended InstSet", null) { InstructionSetWindow("Extended InstSet", InstSet.ep) }

        options.item("Save State", Input.Keys.NUM_0) { saveGb() }
        options.item("Load State", Input.Keys.NUM_1) {
            loadGb()
            gameLoaded()
        }

        menuBar.addMenu(file)
        menuBar.addMenu(view)
        menuBar.addMenu(options)
        root.add(menuBar.table).expandX().fillX().row()
    }

    private fun Menu.item(name: String, shortcut: Int?, click: () -> Unit): MenuItem {
        val item = MenuItem(name, object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) = click()
        })
        if (shortcut != null) {
            item.setShortcut(shortcut)
            hotkeyHandler.register(shortcut, click)
        }
        addItem(item)
        return item
    }

    private fun createFileChooser(): FileChooser {
        FileChooser.setDefaultPrefsName("gamelin")
        FileChooser.setSaveLastDirectory(true)
        val chooser = FileChooser("Choose ROM", FileChooser.Mode.OPEN)
        chooser.selectionMode = FileChooser.SelectionMode.FILES
        val filter = FileTypeFilter(true)
        filter.addRule("GameBoy ROMs (.gb, .gbc)", "gb", "gbc")
        chooser.setFileTypeFilter(filter)
        chooser.setListener(object : StreamingFileChooserListener() {
            override fun selected(file: FileHandle) {
                FileSystem.gamePath = file
                gb.loadGame(file.readBytes())
                gameLoaded()
            }
        })
        return chooser
    }

    private fun toggleWindow(name: String, create: () -> Window) {
        val prev = windows[name]
        when {
            prev?.stage == stage -> prev.fadeOut()
            prev != null -> {
                stage.addActor(prev)
                prev.fadeIn()
            }
            else -> {
                val window = create()
                windows[window.name] = window
                toggleWindow(name, create)
            }
        }
    }

    private fun gameLoaded() {
        saveGameBtn.isDisabled = false
        gbWindow.refresh()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        gbWindow.centerWindow()
    }

    override fun dispose() {
        saveConfiguration()
        gb.dispose()
        stage.dispose()
        exitProcess(0)
    }

    class HotkeyHandler : InputAdapter() {

        private val actions = IntMap<() -> Unit>(10)

        fun register(key: Int, click: () -> Unit) {
            actions[key] = click
        }

        override fun keyDown(keycode: Int): Boolean {
            actions[keycode]?.invoke()
            return true
        }
    }
}
