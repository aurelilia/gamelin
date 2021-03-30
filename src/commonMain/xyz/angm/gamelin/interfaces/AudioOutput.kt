/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 8:49 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

/** An audio output used by the emulator to play the emulated system's sounds to the user. */
expect class AudioOutput() {

    /** Reset the output's internal state; used when the system is reset or game is switched. */
    fun reset()

    /** Queue the given PCM sample for playing, given as an unsigned byte.
     * The rate at which this is called depends on [AudioOutput.SAMPLE_RATE].
     * This method may block once the internal buffer is full to play
     * it to the user; this is platform-dependent:
     * - JVM: Uses libGDX audio; blocks until played; uses audio for general emulator timing
     * - JS: Uses KORGE audio; does not block. */
    fun play(left: Byte, right: Byte)

    companion object {
        /** The rate at which [AudioOutput.play] is to be called with new samples.
         * Usually 22050Hz or 44100Hz. */
        val SAMPLE_RATE: Int
    }
}
