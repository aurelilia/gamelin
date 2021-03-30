/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:24 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import kotlinx.browser.localStorage

actual fun Number.hex8() = (this.toInt() and 0xFF).toString(16)
actual fun Number.hex16() = (this.toInt() and 0xFFFF).toString(16)

/** Convert a byte array to a string; string will not be valid UTF-8! */
fun bytesToString(bytes: ByteArray): String {
    val builder = StringBuilder()
    builder.ensureCapacity(bytes.size)
    for (b in bytes) builder.append(b.toChar())
    return builder.toString()
}

/** Convert a string encoded with [bytesToString] back into a byte array */
fun stringToBytes(str: String) = ByteArray(str.length) { str[it].toByte() }

/** Saving configuration on JS is not supported, saving does nothing. */
actual fun saveConfiguration() {}
/** Saving configuration on JS is not supported, loading creates a new config. */
actual fun loadConfiguration() = Configuration()