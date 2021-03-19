/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/19/21, 11:27 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

// This file's contents are from KorAU, abridged to remove `delay` calls and suspend functions.
package xyz.angm.gamelin.interfaces

import com.soywiz.kds.FloatArrayDeque
import com.soywiz.korau.internal.SampleConvert
import com.soywiz.korau.sound.*
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import kotlinx.browser.document

private val temp = FloatArray(1)

internal fun FloatArrayDeque.write(value: Float) {
    temp[0] = value
    write(temp, 0, 1)
}

val nativeSoundProvider: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }

class JsPlatformAudioOutput {

    init {
        nativeSoundProvider.initOnce()
    }

    private var missingDataCount = 0
    private var nodeRunning = false
    private var node: ScriptProcessorNode? = null
    private var startPromise: Cancellable? = null

    private val nchannels = 2
    private val deques = Array(nchannels) { FloatArrayDeque() }

    private fun process(e: AudioProcessingEvent) {
        val outChannels = Array(e.outputBuffer.numberOfChannels) { e.outputBuffer.getChannelData(it) }
        var hasData = true

        if (!document.asDynamic().hidden as Boolean) {
            for (channel in 0 until nchannels) {
                val deque = deques[channel]
                val outChannel = outChannels[channel]
                val read = deque.read(outChannel)
                if (read < outChannel.size) hasData = false
            }
        }

        if (!hasData) missingDataCount++
        if (missingDataCount >= 500) stop()
    }

    private fun ensureInit() = run { node }


    fun start() {
        if (nodeRunning) return
        startPromise = HtmlSimpleSound.callOnUnlocked {
            node = HtmlSimpleSound.ctx?.createScriptProcessor(1024, 2, 2)
            node?.onaudioprocess = { process(it) }
            if (HtmlSimpleSound.ctx != null) this.node?.connect(HtmlSimpleSound.ctx!!.destination)
        }
        missingDataCount = 0
        nodeRunning = true
    }

    private fun stop() {
        if (!nodeRunning) return
        startPromise?.cancel()
        this.node?.disconnect()
        nodeRunning = false
    }

    private fun ensureRunning() {
        ensureInit()
        if (!nodeRunning) {
            start()
        }
    }

    fun add(samples: AudioSamples, offset: Int, size: Int) {
        ensureRunning()
        val schannels = samples.channels
        for (channel in 0 until nchannels) {
            val sample = samples[channel % schannels]
            val deque = deques[channel]
            for (n in 0 until size) {
                deque.write(SampleConvert.shortToFloat(sample[offset + n]))
            }
        }
    }
}
