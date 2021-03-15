/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 3:21 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.Interrupt

private val row0Keys = arrayOf(Input.Keys.Z, Input.Keys.X, Input.Keys.SPACE, Input.Keys.ENTER).reversedArray()
private val row1Keys = arrayOf(Input.Keys.RIGHT, Input.Keys.LEFT, Input.Keys.UP, Input.Keys.DOWN).reversedArray()

class Keyboard(private val gb: GameBoy) : InputAdapter() {

    private var column = 0

    fun read() = when (column) {
        0x10 -> build(row0Keys)
        0x20 -> build(row1Keys)
        else -> 0
    }.toByte()

    fun write(value: Byte) {
        column = value.int() and 0x30
    }

    private fun build(keys: Array<Int>) = rowKeys(keys) or column or 0b11000000

    private fun rowKeys(keys: Array<Int>): Int {
        var res = 0
        for (key in keys) {
            res = res shl 1
            if (!Gdx.input.isKeyPressed(key)) res++
        }
        return res
    }

    override fun keyDown(keycode: Int): Boolean {
        if ((read().int() and 0x0F) != 0x0F) gb.requestInterrupt(Interrupt.Joypad)
        return false
    }

    fun reset() {
        column = 0
    }
}