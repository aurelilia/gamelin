/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:01 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.utils.GdxRuntimeException
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

class AudioOutput {

    private val SAMPLE_RATE = 22050
    private val BUFFER_SIZE = 1024

    private var device: AudioDevice? = null
    private var counter = 0
    private var divider = CLOCK_SPEED_HZ / SAMPLE_RATE
    private var bufferIndex = 0
    private val buffer = ShortArray(BUFFER_SIZE)
    private var audioThread: AudioThread? = null
    val str = StringBuilder()

    init {
        initialize()
    }

    fun initialize() {
        device = try {
            Gdx.audio.newAudioDevice(SAMPLE_RATE, false)
        } catch (e: GdxRuntimeException) {
            null
        }

        if (device != null) {
            audioThread = AudioThread(device!!)
            audioThread!!.start()
        }
    }

    fun reset() {
        counter = 0
        bufferIndex = 0
        buffer.fill(0)
    }

    fun play(left: Byte, right: Byte) {
        if (counter++ != 0) {
            counter %= divider
            return
        }
        if (left != 0.toByte()) {
            str.appendLine(left)
        }

        buffer[bufferIndex++] = (left * 25).toShort()
        buffer[bufferIndex++] = (right * 25).toShort()

        if (bufferIndex >= BUFFER_SIZE) {
            audioThread?.buffer = buffer
            audioThread?.play = true

            bufferIndex = 0
        }
    }

    fun dispose() {
        Gdx.files.local("sound").writeString(str.toString(), false)
        device?.dispose()
    }
}

class AudioThread(private val device: AudioDevice) : Thread() {

    @Volatile
    var play = false
        @Synchronized set
    var buffer = ShortArray(1024)

    override fun run() {
        while (true) {
            if (play) {
                device.writeSamples(buffer, 0, 1024)
                play = false
            }
        }
    }
}