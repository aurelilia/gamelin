/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:28 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class AudioOutput {

    private var device = Gdx.audio.newAudioDevice(SAMPLE_RATE, false)
    private var counter = 0
    private var bufferIndex = 0
    private val buffer = ShortArray(BUFFER_SIZE)

    @Volatile
    var play = false
        @Synchronized set

    init {
        Thread {
            while (true) {
                if (play) {
                    device.writeSamples(buffer, 0, buffer.size)
                    play = false
                }
            }
        }.start()
    }

    fun reset() {
        counter = 0
        bufferIndex = 0
        buffer.fill(0)
    }

    fun play(left: Byte, right: Byte) {
        if (counter++ != 0) {
            counter %= DIVIDER
            return
        }

        buffer[bufferIndex++] = (left * 16).toShort()
        buffer[bufferIndex++] = (right * 16).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            play = true
            bufferIndex = 0
        }
    }

    fun dispose() = device.dispose()

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = (2048 * (SAMPLE_RATE / 22050f)).toInt()
        private const val DIVIDER = CLOCK_SPEED_HZ / SAMPLE_RATE
    }
}
