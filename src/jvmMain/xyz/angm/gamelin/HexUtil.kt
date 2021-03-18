/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:25 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

actual fun Number.hex8() = String.format("0x%02X", this)
actual fun Number.hex16() = String.format("0x%04X", this)
