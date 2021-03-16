/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:17 PM.
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
    private var buffer = ShortArray(BUFFER_SIZE)

    @Volatile
    private var play = false
        @Synchronized set

    init {
        Thread {
            while (true) {
                if (play) {
                    device.writeSamples(buffer, 0, BUFFER_SIZE)
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
        buffer[bufferIndex++] = (left * 32).toShort()
        buffer[bufferIndex++] = (right * 32).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            play = true
            bufferIndex = 0
        }
    }

    fun needsSamples() = !play

    fun dispose() = device.dispose()

    companion object {
        const val SAMPLE_RATE = 22050
        const val BUFFER_SIZE = 1024
        const val DIVIDER = CLOCK_SPEED_HZ / SAMPLE_RATE
    }
}
