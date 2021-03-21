/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:28 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.soywiz.korau.sound.AudioSamples
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

/** Sound enable checkbox. */
private val soundEnable = document.getElementById("sound-enable") as HTMLInputElement

/** Audio output using KORGE audio.
 * TODO: Not great: Buffer underruns crackle and happen often due to JS being slow */
actual class AudioOutput actual constructor() {

    private val device = JsPlatformAudioOutput()
    private var buffer = AudioSamples(2, BUFFER_SIZE)
    private var bufferIndex = 0
    private var enabled = soundEnable.checked

    init {
        device.start()
        soundEnable.addEventListener("change", {
            enabled = soundEnable.checked
        })
    }

    actual fun reset() {
        buffer = AudioSamples(2, BUFFER_SIZE)
        bufferIndex = 0
    }

    actual fun play(left: Byte, right: Byte) {
        if (!enabled) return
        // For some reason, the output just ignores the given sample rate and forces 44100hz.
        // Just double it manually to emulate 22050hz
        buffer[0, bufferIndex] = (left * 32).toShort()
        buffer[1, bufferIndex++] = (right * 32).toShort()
        buffer[0, bufferIndex] = (left * 32).toShort()
        buffer[1, bufferIndex++] = (right * 32).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            device.add(buffer, 0, buffer.totalSamples)
            bufferIndex = 0
        }
    }

    actual companion object {
        actual val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 2048
    }
}