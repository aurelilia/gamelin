/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:26 PM.
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

/** Display a byte as padded hex, format `0xXX` */
expect fun Number.hex8(): String
/** Display 2 bytes as padded hex, format `0xXXXX` */
expect fun Number.hex16(): String

/** Simple interface for classes that hold some form of platform/native
 * resouces that need to be disposed manually.
 * Usually, object become unusable after calling dispose. */
interface Disposable {
    fun dispose()
}

/** Save [configuration] to platform-appropriate storage. */
expect fun saveConfiguration()
/** Load [configuration] from platform storage. */
expect fun loadConfiguration(): Configuration
