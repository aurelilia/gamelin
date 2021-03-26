/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 3:45 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

val configuration = loadConfiguration()

/** Emulator-level configuration, to be saved/loaded by the platform.
 * @property preferCGB Prefer running games that support GB and GBC in GBC mode
 * @property cgbColorCorrection If CGB colors should be 'corrected' or simply be 1:1 translated. */
class Configuration {
    var preferCGB = true
    var cgbColorCorrection = false
}