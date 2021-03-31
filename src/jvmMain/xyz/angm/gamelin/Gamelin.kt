/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/31/21, 7:11 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.*
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.freetype.FreeType
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
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
import xyz.angm.gamelin.interfaces.*
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.InstSet
import xyz.angm.gamelin.windows.AboutWindow
import xyz.angm.gamelin.windows.CartInfoWindow
import xyz.angm.gamelin.windows.GameBoyWindow
import xyz.angm.gamelin.windows.Window
import xyz.angm.gamelin.windows.debugger.*
import xyz.angm.gamelin.windows.options.OptionsWindow

/** The global GameBoy instance the program is currently operating on.
 * This was decided to be implmeneted as global state to allow switching out the
 * GB at any point, for features like save states. */
var gb = GameBoy(DesktopDebugger())
/** If [gb] is currently being rewinded. Will cause some behavior changes in [TileRenderer]
 * to allow producing a frame of each rewind state, see [Gamelin.render] and [TileRenderer.finishFrame]
 * for details. */
@Volatile
var rewinding = false
    @Synchronized set(value) {
        field = value && config.enableRewind
    }

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
        loadSkin()
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
        Controllers.addListener(hotkeyHandler)

        gbWindow = GameBoyWindow()
        stage.addActor(gbWindow)
        stage.addActor(root)

        root.setFillParent(true)
        menuBarCell = root.add().expandX().fillX()
        root.row()
        recreateMenus()
        root.add().expand().fill()

        createHotKey(
            "Fast Forward (Hold)", Input.Keys.C,
            { gb.mmu.sound.output.skip += config.fastForwardHoldSpeed },
            { gb.mmu.sound.output.skip -= config.fastForwardHoldSpeed }
        )
        var ffToggled = false
        createHotKey(
            "Fast Forward (Toggle)", Input.Keys.TAB,
            {
                if (ffToggled) gb.mmu.sound.output.skip -= config.fastForwardHoldSpeed
                else gb.mmu.sound.output.skip += config.fastForwardHoldSpeed
                ffToggled = !ffToggled
            },
        )
        createHotKey(
            "Rewind", Input.Keys.H,
            { rewinding = true }, { Gdx.app.postRunnable { SaveState.endRewind() } }
        )
        hotkeyHandler.update()

        Thread {
            while (!gb.disposed) {
                if (rewinding) Thread.sleep(16)
                else gb.advanceIndefinitely { Thread.sleep(16) }
            }
        }.start()
    }

    private fun loadSkin() {
        if (config.skin.path != null) VisUI.load(file(config.skin.path!!)) else VisUI.load()
        val monospaceGen = FreeTypeFontGenerator(file("noto-sans-mono-light.ttf"))
        VisUI.getSkin().apply {
            val monospace = monospaceGen.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                size = 15
                mono = false
            })
            add("monospace", Label.LabelStyle().apply { font = monospace })
        }
    }

    private fun recreateMenus() {
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
        if (config.hotkeyButtons[name] == null) config.hotkeyButtons[name] = -1

        hotkeyHandler.register(name, Action(exec, up))
        return shortcut
    }

    private fun createFileChooser(): FileChooser {
        FileChooser.setDefaultPrefsName("gamelin")
        FileChooser.setSaveLastDirectory(true)
        val chooser = FileChooser("Choose ROM", FileChooser.Mode.OPEN)
        Window.padCloseButton(chooser)
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

    fun toggleWindow(name: String, create: () -> Window) {
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

    private var rewindFrame = false

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        rewindFrame = !rewindFrame
        // If rewinding, load the next rewind state and 'advance until debug halt' -
        // this will advance until `TileRenderer.finishFrame` halts the emu,
        // which is required to have a frame to display to the user
        // (otherwise rewinding would just hang display output, which would be rather useless)
        if (rewinding && rewindFrame) {
            gb.advanceUntilDebugHalt()
            gb.debugger.emuHalt = false
            SaveState.rewindNext()
        }

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
     * See [Application.cleanupWindows] for an explanation of why this is needed. */
    internal fun beforeExit() {
        gb.debugger.emuHalt = true
        saveConfiguration()
    }

    override fun dispose() {
        gb.dispose()
        stage.dispose()
        device.dispose()
    }

    internal class HotkeyHandler : InputProcessor by InputAdapter(), ControllerListener by ControllerAdapter() {

        private val actions = ObjectMap<String, Action>(10)
        private val keyToActions = IntMap<Action>(10)
        private val buttonToActions = IntMap<Action>(10)

        internal fun register(name: String, click: Action) {
            actions[name] = click
        }

        fun update() {
            keyToActions.clear()
            for (action in actions) {
                val key = config.hotkeys[action.key]
                if (key != null) keyToActions[key] = action.value

                val button = config.hotkeyButtons[action.key]
                if (button != null) buttonToActions[button] = action.value
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

        override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
            val action = buttonToActions[buttonCode]
            action?.keyDown?.invoke()
            return action != null
        }

        override fun buttonUp(controller: Controller?, buttonCode: Int): Boolean {
            val action = buttonToActions[buttonCode]
            action?.keyUp?.invoke()
            return action != null
        }
    }

    internal class Action(val keyDown: () -> Unit, val keyUp: (() -> Unit)?)
}
