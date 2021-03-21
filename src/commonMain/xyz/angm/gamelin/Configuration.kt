/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:24 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

val configuration = loadConfiguration()

/** Emulator-level configuration, to be saved/loaded by the platform.
 * @property cgbColorCorrection If CGB colors should be 'corrected' or simply be 1:1 translated. */
class Configuration {
    var cgbColorCorrection = false
}