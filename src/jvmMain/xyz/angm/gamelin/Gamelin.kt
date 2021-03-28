/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 9:25 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.*
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.ToastManager
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import com.kotcrab.vis.ui.widget.file.StreamingFileChooserListener
import com.kotcrab.vis.ui.widget.toast.ToastTable
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.assets.file
import ktx.collections.*
import ktx.scene2d.vis.menuItem
import ktx.scene2d.vis.subMenu
import xyz.angm.gamelin.interfaces.DesktopDebugger
import xyz.angm.gamelin.interfaces.FileSystem
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.windows.*

/** The global GameBoy instance the program is currently operating on.
 * This was decided to be implmeneted as global state to allow switching out the
 * GB at any point, for features like save states. */
var gb = GameBoy(DesktopDebugger())

/** The libGDX application driving the desktop emulator.
 * @property disabledButtons Buttons that are only enabled once a game is loaded. */
class Gamelin : ApplicationAdapter() {

    private lateinit var stage: Stage
    private lateinit var root: VisTable
    private lateinit var menuBarCell: Cell<Actor>
    private lateinit var toasts: ToastManager
    private val windows = HashMap<String, Window>()
    internal val hotkeyHandler = HotkeyHandler()
    private lateinit var gbWindow: GameBoyWindow
    private val disabledButtons = GdxArray<MenuItem>()

    override fun create() {
        if (config.skin.path != null) VisUI.load(file(config.skin.path!!)) else VisUI.load()
        stage = Stage(com.badlogic.gdx.utils.viewport.ScreenViewport())
        root = VisTable()
        toasts = ToastManager(stage)
        toasts.alignment = Align.bottomRight

        val multi = InputMultiplexer()
        multi.addProcessor(Keyboard)
        multi.addProcessor(stage)
        multi.addProcessor(hotkeyHandler)
        Gdx.input.inputProcessor = multi
        Controllers.addListener(Keyboard)

        gbWindow = GameBoyWindow()
        stage.addActor(gbWindow)
        stage.addActor(root)

        root.setFillParent(true)
        menuBarCell = root.add().expandX().fillX()
        root.row()
        recreateMenus()
        root.add().expand().fill()

        createHotKey("Fast Forward", Input.Keys.C, { gb.mmu.sound.output.skip = config.fastForwardSpeed }) { gb.mmu.sound.output.skip = 0 }
        hotkeyHandler.update()

        Thread {
            while (!gb.disposed) {
                gb.advanceIndefinitely { Thread.sleep(16) }
            }
        }.start()
    }

    fun recreateMenus() {
        val menuBar = MenuBar()
        val file = Menu("File")
        val debugger = Menu("Debugger")
        val options = Menu("Options")

        val chooser = createFileChooser()
        file.item("Load ROM", Input.Keys.O) {
            stage.addActor(chooser)
            chooser.fadeIn()
        }
        val lastFiles = MenuItem("Last Opened ROMs")
        file.addItem(lastFiles)
        lastFiles.subMenu {
            for (path in config.lastOpened.reversed()) menuItem(path) {
                onChange {
                    val file = Gdx.files.absolute(path)
                    if (file.exists()) loadGame(file)
                    else toast("File has been moved or deleted.")
                }
            }
        }

        file.addSeparator()
        file.item("Pause", Input.Keys.P, disable = true) { gb.debugger.emuHalt = !gb.debugger.emuHalt }
        file.item("Reset", Input.Keys.R, disable = true) { resetButton() }
        file.item("Save Game to disk", Input.Keys.S, disable = true) {
            runInGbThread { gb.mmu.cart.save() }
            toast("Game saved.")
        }
        file.addSeparator()
        file.item("Exit", Input.Keys.F4) { Gdx.app.exit() }

        fun windowItem(name: String, shortcut: Int?, menu: Menu = debugger, disable: Boolean = false, create: () -> Window) {
            menu.item(name, shortcut, disable) { toggleWindow(name, create) }
        }

        windowItem("Debugger", Input.Keys.F7, disable = true) { DebuggerWindow() }
        debugger.addSeparator()
        windowItem("BG Map Viewer", Input.Keys.F8) { BGMapViewer() }
        windowItem("VRAM Viewer", Input.Keys.F9) { VRAMViewer() }
        windowItem("Cartridge Info", Input.Keys.I) { CartInfoWindow() }
        debugger.addSeparator()
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
                onChange {
                    FileSystem.saveState(i.toString())
                    toast("Saved to slot $i.")
                }
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
                    toast("Loaded slot $i.")
                }
            }
        }
        options.item("Undo last load", null, disable = true) {
            FileSystem.loadState("last")
            toast("Undid loading save state.")
        }
        options.addSeparator()
        windowItem("About", null, options) { AboutWindow() }

        menuBar.addMenu(file)
        menuBar.addMenu(debugger)
        menuBar.addMenu(options)
        menuBarCell.setActor(menuBar.table)
    }

    private inline fun Menu.item(name: String, defaultShortcut: Int?, disable: Boolean = false, crossinline click: () -> Unit): MenuItem {
        val item = MenuItem(name, object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) = click()
        })
        val shortcut = createHotKey(name, defaultShortcut ?: -1, { if (!item.isDisabled) click() })
        if (shortcut != -1) item.setShortcut(shortcut)

        if (disable) {
            disabledButtons.add(item)
            item.isDisabled = true
        }
        addItem(item)
        return item
    }

    private fun createHotKey(name: String, default: Int, exec: () -> Unit, up: (() -> Unit)? = null): Int {
        val key = config.hotkeys[name]
        val shortcut = if (key == null) {
            config.hotkeys[name] = default
            default
        } else key

        hotkeyHandler.register(name, Action(exec, up))
        return shortcut
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
            override fun selected(file: FileHandle) = loadGame(file)
        })
        return chooser
    }

    private fun loadGame(file: FileHandle) {
        val absolutePath = file.file().absolutePath
        config.lastOpened.remove(absolutePath)
        config.lastOpened.add(absolutePath)
        if (config.lastOpened.size > 10) config.lastOpened.removeIndex(0)
        recreateMenus()

        FileSystem.gamePath = file
        toast("Loaded '${file.name()}'.")
        runInGbThread {
            gb.loadGame(file.readBytes())
            for (btn in disabledButtons) btn.isDisabled = false
            gbWindow.refresh()
        }
    }

    private fun resetButton() {
        if (!config.confirmResets) {
            runInGbThread { gb.reset() }
            toast("Reset console.")
            return
        }

        val toast = ToastTable()
        toast.add("Are you sure you want to reset?").colspan(2).row()
        toast.add(VisTextButton("Yes").apply {
            onClick {
                runInGbThread { gb.reset() }
                toast("Reset console.")
                toast.fadeOut()
            }
        }).pad(5f).width(40f)
        toast.add(VisTextButton("No").apply {
            onClick { toast.fadeOut() }
        }).pad(5f).width(40f)
        toast.pad(10f)
        toasts.show(toast, 10f)
    }

    internal fun toast(msg: String) {
        val table = VisTable()
        table.add(msg).pad(10f)
        toasts.show(table, 3f)
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
        toasts.resize()
    }

    /** Stops the GB emulator and saves configuration.
     * See [Application.cleanupWindows] for an explaination of why this is needed. */
    internal fun beforeExit() {
        gb.debugger.emuHalt = true
        saveConfiguration()
    }

    override fun dispose() {
        gb.dispose()
        stage.dispose()
    }

    internal class HotkeyHandler : InputAdapter() {

        private val actions = ObjectMap<String, Action>(10)
        private val keyToActions = IntMap<Action>(10)

        internal fun register(name: String, click: Action) {
            actions[name] = click
        }

        fun update() {
            keyToActions.clear()
            for (action in actions) {
                val key = config.hotkeys[action.key] ?: continue
                keyToActions[key] = action.value
            }
        }

        override fun keyDown(keycode: Int): Boolean {
            val action = keyToActions[keycode]
            action?.keyDown?.invoke()
            return action != null
        }

        override fun keyUp(keycode: Int): Boolean {
            val action = keyToActions[keycode]
            action?.keyUp?.invoke()
            return action != null
        }
    }

    internal class Action(val keyDown: () -> Unit, val keyUp: (() -> Unit)?)
}
