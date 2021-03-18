/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 10:48 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

actual class AudioOutput actual constructor() {
    actual fun reset() {
    }

    actual fun play(left: Byte, right: Byte) {
    }

    actual fun needsSamples(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun dispose() {
    }

    actual companion object {
        actual val SAMPLE_RATE: Int
            get() = TODO("Not yet implemented")
        actual val BUFFER_SIZE: Int
            get() = TODO("Not yet implemented")
        actual val DIVIDER: Int
            get() = TODO("Not yet implemented")
    }

}