/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:54 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

expect class AudioOutput() {

    fun reset()
    fun play(left: Byte, right: Byte)
    fun needsSamples(): Boolean
    fun dispose()

    companion object {
        val SAMPLE_RATE: Int
        val BUFFER_SIZE: Int
        val DIVIDER: Int
    }
}
