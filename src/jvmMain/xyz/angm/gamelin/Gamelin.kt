/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 9:35 PM.
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
import ktx.actors.onChange
import ktx.collections.*
import ktx.scene2d.vis.menuItem
import ktx.scene2d.vis.subMenu
import xyz.angm.gamelin.interfaces.DesktopDebugger
import xyz.angm.gamelin.interfaces.FileSystem
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.windows.*
import kotlin.system.exitProcess

/** The global GameBoy instance the program is currently operating on.
 * This was decided to be implmeneted as global state to allow switching out the
 * GB at any point, for features like save states. */
var gb = GameBoy(DesktopDebugger())

/** The libGDX application driving the desktop emulator.
 * @property disabledButtons Buttons that are only enabled once a game is loaded. */
class Gamelin : ApplicationAdapter() {

    private lateinit var stage: Stage
    private val windows = HashMap<String, Window>()
    private val hotkeyHandler = HotkeyHandler()
    private lateinit var gbWindow: GameBoyWindow
    private val disabledButtons = GdxArray<MenuItem>()

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
        val debugger = Menu("Debugger")
        val options = Menu("Options")

        val chooser = createFileChooser()
        file.item("Load ROM", Input.Keys.O) {
            stage.addActor(chooser)
            chooser.fadeIn()
        }
        file.addSeparator()
        file.item("Pause", Input.Keys.P, disable = true) { gb.debugger.emuHalt = !gb.debugger.emuHalt }
        file.item("Reset", Input.Keys.R, disable = true) { gb.reset() }
        file.item("Save Game to disk", Input.Keys.S, disable = true) { gb.mmu.cart.save() }
        file.addSeparator()
        file.item("Exit", Input.Keys.F4) { Gdx.app.exit() }

        fun windowItem(name: String, shortcut: Int?, menu: Menu = debugger, disable: Boolean = false, create: () -> Window) {
            menu.item(name, shortcut, disable) { toggleWindow(name, create) }
        }

        windowItem("Debugger", Input.Keys.F7, disable = true) { DebuggerWindow() }
        windowItem("BG Map Viewer", Input.Keys.F8) { BGMapViewer() }
        windowItem("VRAM Viewer", Input.Keys.F9) { VRAMViewer() }
        windowItem("Cartridge Info", Input.Keys.I) { CartInfoWindow() }
        windowItem("Instruction Set", null) { InstructionSetWindow("Instruction Set", InstSet.op) }
        windowItem("Extended InstSet", null) { InstructionSetWindow("Extended InstSet", InstSet.ep) }

        windowItem("Options", Input.Keys.F10, options) { OptionsWindow(this) }
        options.addSeparator()
        val saveState = MenuItem("Save State")
        options.addItem(saveState)
        saveState.subMenu {
            for (i in 0 until 10) menuItem("Slot $i") {
                disabledButtons.add(this)
                isDisabled = true
                onChange { FileSystem.saveState(i.toString()) }
            }
        }
        val loadState = MenuItem("Load State")
        options.addItem(loadState)
        loadState.subMenu {
            for (i in 0 until 10) menuItem("Slot $i") {
                disabledButtons.add(this)
                isDisabled = true
                onChange {
                    FileSystem.loadState(i.toString())
                    gameLoaded()
                }
            }
        }
        options.item("Undo last load", null, disable = true) {
            FileSystem.loadState("last")
            gameLoaded()
        }

        menuBar.addMenu(file)
        menuBar.addMenu(debugger)
        menuBar.addMenu(options)
        root.add(menuBar.table).expandX().fillX().row()
    }

    private inline fun Menu.item(name: String, shortcut: Int?, disable: Boolean = false, crossinline click: () -> Unit): MenuItem {
        val item = MenuItem(name, object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) = click()
        })
        if (shortcut != null) {
            item.setShortcut(shortcut)
            hotkeyHandler.register(shortcut) { if (!item.isDisabled) click() }
        }
        if (disable) {
            disabledButtons.add(item)
            item.isDisabled = true
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
        for (btn in disabledButtons) btn.isDisabled = false
        gbWindow.refresh()
    }

    internal fun reloadGameWindow() {
        gbWindow.fadeOut()
        gbWindow = GameBoyWindow()
        stage.addActor(gbWindow)
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        gb.mmu.ppu.renderer.beforeRender()
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
        Thread.sleep(20)
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
