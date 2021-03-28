/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 3:41 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

/** Sound enable checkbox. */
private val soundEnable = document.getElementById("sound-enable") as HTMLInputElement
lateinit var device: JsPlatformAudioOutput

/** Audio output using KORGE audio. */
actual class AudioOutput actual constructor() {

    private var bufferL = FloatArray(BUFFER_SIZE)
    private var bufferR = FloatArray(BUFFER_SIZE)
    private var bufferIndex = 0
    private var enabled = soundEnable.checked

    init {
        device = JsPlatformAudioOutput()
        device.start()
        soundEnable.addEventListener("change", {
            enabled = soundEnable.checked
        })
    }

    actual fun reset() {
        bufferIndex = 0
    }

    actual fun play(left: Byte, right: Byte) {
        // For some reason, the output just ignores the given sample rate and forces 44100hz.
        // Just double it manually to emulate 22050hz
        if (enabled) {
            val lf = left / 2048f
            val rf = right / 2048f
            bufferL[bufferIndex] = lf
            bufferR[bufferIndex++] = rf
            bufferL[bufferIndex] = lf
            bufferR[bufferIndex++] = rf
        } else {
            bufferL[bufferIndex] = 0f
            bufferR[bufferIndex++] = 0f
            bufferL[bufferIndex] = 0f
            bufferR[bufferIndex++] = 0f
        }
        if (bufferIndex >= BUFFER_SIZE) {
            device.add(bufferL, bufferR)
            bufferIndex = 0
        }
    }

    actual companion object {
        actual val SAMPLE_RATE = 22050
        const val BUFFER_SIZE = 2048
    }
}