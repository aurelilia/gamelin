/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 3:08 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import xyz.angm.gamelin.config

private var device = Gdx.audio.newAudioDevice(AudioOutput.SAMPLE_RATE, false).apply {
    setVolume(config.volume)
}

/** Audio output using libGDX audio devices. Will block on [play] to allow
 * synchronizing the entire emulator around the audio buffer.
 * @property skip Set this property to to only buffer every $(skip + 1)th sample, making the game run at
 * multiples of it's regular speed due to being timed against audio. Used to implement fast-forward. */
actual class AudioOutput actual constructor() {

    @Transient
    private var bufferIndex = 0
    @Transient
    private var buffer = ShortArray(BUFFER_SIZE)
    @Transient
    var skip = 0
        set(value) {
            field = value
            device.setVolume(if (value > 0) config.fastForwardVolume else config.volume)
        }
    @Transient
    private var skipIndex = 0

    actual fun reset() {
        bufferIndex = 0
        buffer.fill(0)
        skip = 0
        skipIndex = 0
    }

    actual fun play(left: Byte, right: Byte) {
        if (skip == 0 || skipIndex++ == skip) {
            skipIndex = 0
            buffer[bufferIndex++] = (left * 64).toShort()
            buffer[bufferIndex++] = (right * 64).toShort()
            if (bufferIndex >= BUFFER_SIZE) {
                device.writeSamples(buffer, 0, BUFFER_SIZE)
                bufferIndex = 0
            }
        }
    }

    actual companion object {
        actual val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 1024
    }
}
