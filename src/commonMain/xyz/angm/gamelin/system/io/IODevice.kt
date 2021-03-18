/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:15 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.int

abstract class IODevice {
    abstract fun read(addr: Int): Int
    abstract fun write(addr: Int, value: Int)
    fun read(addr: Short) = read(addr.int()).toByte()
    fun write(addr: Short, value: Byte) = write(addr.int(), value.int())
}