/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 9:13 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

fun Byte.bit(bit: Int) = this.int() and (1 shl bit)
fun Byte.isBit(bit: Int) = (this.int() and (1 shl bit)) shr bit
fun Byte.rotRight(dist: Int): Byte = ((this.int() ushr dist) or (this.int() shl (8 - dist))).toByte()
fun Byte.rotLeft(dist: Int): Byte = ((this.int() shl dist) or (this.int() ushr (8 - dist))).toByte()

fun Byte.int() = this.toInt() and 0xFF
fun Short.int() = this.toInt() and 0xFFFF

fun Int.isBit(bit: Int) = ((this and (1 shl bit)) shr bit) == 1

fun Number.hex8() = String.format("0x%02X", this)
fun Number.hex16() = String.format("0x%04X", this)
