/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/27/21, 11:18 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

// This file's contents are from KorAU, abridged to remove `delay` calls and suspend functions.
package xyz.angm.gamelin.interfaces

import com.soywiz.kds.FloatArrayDeque
import com.soywiz.korau.sound.*
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import kotlinx.browser.document

val nativeSoundProvider: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }
var gotSamples = false

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

    fun add(samplesL: FloatArray, samplesR: FloatArray) {
        ensureRunning()
        deques[0].write(samplesL, 0, samplesL.size)
        deques[1].write(samplesR, 0, samplesR.size)
        gotSamples = deques[0].availableRead > samplesL.size * 3
    }
}
