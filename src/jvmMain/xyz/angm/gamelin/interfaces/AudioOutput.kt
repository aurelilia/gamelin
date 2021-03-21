/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:32 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx

private val device = Gdx.audio.newAudioDevice(AudioOutput.SAMPLE_RATE, false)

/** Audio output using libGDX audio devices. Will block on [play] to allow
 * synchronizing the entire emulator around the audio buffer. */
actual class AudioOutput actual constructor() {

    @Transient
    private var bufferIndex = 0
    @Transient
    private var buffer = ShortArray(BUFFER_SIZE)

    actual fun reset() {
        bufferIndex = 0
        buffer.fill(0)
    }

    actual fun play(left: Byte, right: Byte) {
        buffer[bufferIndex++] = (left * 32).toShort()
        buffer[bufferIndex++] = (right * 32).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            device.writeSamples(buffer, 0, BUFFER_SIZE)
            bufferIndex = 0
        }
    }

    actual companion object {
        actual val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 1024
    }
}
