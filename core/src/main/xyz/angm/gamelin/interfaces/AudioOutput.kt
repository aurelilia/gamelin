/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:32 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class AudioOutput {

    private var source = Gdx.audio.newAudioDevice(SAMPLE_RATE, false)
    private var buffer = ShortArray(BUFFER_SIZE)
    private var bufferIdx = 0
    private var counter = 0
    private val audioThread = AudioThread(source, buffer)

    init {
        audioThread.start()
    }

    fun play(left: Int, right: Int) {
        if (counter++ != 0) {
            counter %= DIVIDER
            return
        }

        buffer[bufferIdx++] = (left.toByte() * 255).toShort()
        buffer[bufferIdx++] = (right.toByte() * 255).toShort()
        if (bufferIdx >= BUFFER_SIZE) {
            audioThread.play = true
            bufferIdx = 0
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val DIVIDER = (CLOCK_SPEED_HZ / SAMPLE_RATE)
        internal const val BUFFER_SIZE = 1024
    }
}

class AudioThread(private val device: AudioDevice, private val buffer: ShortArray) : Thread() {

    @Volatile var play = false
        @Synchronized set

    override fun run() {
        while (true) {
            if (play) {
                device.writeSamples(buffer, 0, AudioOutput.BUFFER_SIZE)
                play = false
            }
        }
    }
}