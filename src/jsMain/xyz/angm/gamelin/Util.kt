/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 3:18 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import kotlinx.browser.localStorage

actual fun Number.hex8() = (this.toInt() and 0xFF).toString(16)
actual fun Number.hex16() = (this.toInt() and 0xFFFF).toString(16)

fun bytesToString(bytes: ByteArray): String {
    val builder = StringBuilder()
    builder.ensureCapacity(bytes.size)
    for (b in bytes) builder.append(b.toChar())
    return builder.toString()
}

fun stringToBytes(str: String) = ByteArray(str.length) { str[it].toByte() }

actual fun saveConfiguration() {
    localStorage.setItem("config", JSON.stringify(configuration))
}

actual fun loadConfiguration(): Configuration {
    return JSON.parse(localStorage.getItem("config") ?: return Configuration()) as Configuration
}