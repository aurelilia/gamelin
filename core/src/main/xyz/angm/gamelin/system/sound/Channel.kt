/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:22 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.system.GameBoy

abstract class Channel(protected val gb: GameBoy, val offset: Int, length: Int) {

    protected open var nr0 = 0
    protected open var nr1 = 0
    protected open var nr2 = 0
    protected open var nr3 = 0
    protected open var nr4 = 0
        set(value) {
            field = value
            length.setNr4(value)
            if (value and (1 shl 7) != 0) {
                channelEnabled = dacEnabled
                trigger()
            }
        }

    protected var channelEnabled = false
    protected var dacEnabled = false

    protected val length = LengthCounter(length)


    abstract fun start()
    abstract fun cycle(): Int

    fun stop() {
        channelEnabled = false
    }

    protected abstract fun trigger()

    fun write(address: Short, value: Int) {
        when ((address - offset) and 0xFF) {
            0 -> nr0 = value
            1 -> nr1 = value
            2 -> nr2 = value
            3 -> nr3 = value
            4 -> nr4 = value
        }
    }

    fun read(address: Short): Int {
        return when ((address - offset) and 0xFF) {
            0 -> nr0
            1 -> nr1
            2 -> nr2
            3 -> nr3
            4 -> nr4
            else -> error("Illegal argument")
        }
    }

    fun isEnabled(): Boolean {
        return channelEnabled && dacEnabled
    }

    protected fun getFrequency() = 2048 - (nr3 or (nr4 and 7 shl 8))

    protected open fun updateLength(): Boolean {
        length.cycle()
        if (!length.isEnabled) return channelEnabled
        if (channelEnabled && length.value == 0) channelEnabled = false
        return channelEnabled
    }
}