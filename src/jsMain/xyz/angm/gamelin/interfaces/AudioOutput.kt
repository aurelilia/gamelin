/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 10:48 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.soywiz.korau.sound.AudioSamples
import view
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

actual class AudioOutput actual constructor() {

    private val device = JsPlatformAudioOutput(view.coroutineContext, 44100)
    private var buffer = AudioSamples(2, BUFFER_SIZE)
    private var bufferIndex = 0
    private var play = false

    init {
        device.start()
    }

    actual fun reset() {
        buffer = AudioSamples(2, BUFFER_SIZE)
        bufferIndex = 0
    }

    actual fun play(left: Byte, right: Byte) {
        buffer[0, bufferIndex] = (left * 32).toShort()
        buffer[1, bufferIndex++] = (right * 32).toShort()
        buffer[0, bufferIndex] = (left * 32).toShort()
        buffer[1, bufferIndex++] = (right * 32).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            device.add(buffer, 0, buffer.totalSamples)
            bufferIndex = 0
        }
    }

    actual fun needsSamples() = !play

    actual fun dispose() {
    }

    actual companion object {
        actual val SAMPLE_RATE get() = 22050
        actual val BUFFER_SIZE get() = 8192 * 8
        actual val DIVIDER get() = CLOCK_SPEED_HZ / SAMPLE_RATE
    }
}