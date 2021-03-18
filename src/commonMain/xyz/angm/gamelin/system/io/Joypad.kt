/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:37 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.system.cpu.Interrupt

private val row0Btns = arrayOf(Button.A, Button.B, Button.Select, Button.Start).reversedArray()
private val row1Btns = arrayOf(Button.Right, Button.Left, Button.Up, Button.Down).reversedArray()

internal class Joypad(private val mmu: MMU) {

    private var column = 0

    init {
        Keyboard.buttonPressed = {
            if ((read().int() and 0x0F) != 0x0F) mmu.requestInterrupt(Interrupt.Joypad)
        }
    }

    fun read() = when (column) {
        0x10 -> build(row0Btns)
        0x20 -> build(row1Btns)
        else -> 0
    }.toByte()

    fun write(value: Byte) {
        column = value.int() and 0x30
    }

    private fun build(keys: Array<Button>) = rowKeys(keys) or column or 0b11000000

    private fun rowKeys(keys: Array<Button>): Int {
        var res = 0
        for (key in keys) {
            res = res shl 1
            if (!Keyboard.isPressed(key)) res++
        }
        return res
    }

    fun reset() {
        column = 0
    }
}

enum class Button {
    A, B, Start, Select, Right, Left, Up, Down
}