/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 6:31 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

fun Byte.bit(bit: Int) = this.toInt() and (1 shl bit)
fun Byte.rotRight(dist: Int): Byte = ((this.toInt() ushr dist) or (this.toInt() shl (8 - dist))).toByte()
fun Byte.rotLeft(dist: Int): Byte = ((this.toInt() shl dist) or (this.toInt() ushr (8 - dist))).toByte()
