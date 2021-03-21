/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 3:16 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

fun Byte.bit(bit: Int) = (this.int() and (1 shl bit)) shr bit
fun Byte.isBit(bit: Int) = (this.int() and (1 shl bit)) != 0
fun Byte.setBit(bit: Int, bitState: Int) = (this.int() and ((1 shl bit) xor 0xFF)) or (bitState shl bit)
fun Byte.rotRight(dist: Int): Byte = ((this.int() ushr dist) or (this.int() shl (8 - dist))).toByte()
fun Byte.rotLeft(dist: Int): Byte = ((this.int() shl dist) or (this.int() ushr (8 - dist))).toByte()

fun Byte.int() = this.toInt() and 0xFF
fun Short.int() = this.toInt() and 0xFFFF

fun Int.bit(bit: Int) = (this and (1 shl bit)) shr bit
fun Int.isBit(bit: Int) = (this and (1 shl bit)) != 0
fun Int.setBit(bit: Int, bitState: Int) = (this and ((1 shl bit) xor 0xFF)) or (bitState shl bit)
fun Int.setBit(bit: Int) = setBit(bit, 1)
fun Int.setBit(bit: Int, bitState: Boolean) = (this and ((1 shl bit) xor 0xFF)) or (if (bitState) 1 shl bit else 0)

fun Boolean.int() = if (this) 1 else 0

expect fun Number.hex8(): String
expect fun Number.hex16(): String

fun addrOutOfBounds(addr: Int): Nothing = throw IllegalArgumentException("Index out of bounds: ${addr.hex16()}")

interface Disposable {
    fun dispose()
}

expect fun saveConfiguration()
expect fun loadConfiguration(): Configuration
