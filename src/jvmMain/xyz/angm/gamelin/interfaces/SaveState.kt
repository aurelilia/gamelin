/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import xyz.angm.gamelin.config
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.rewinding
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.CPU
import xyz.angm.gamelin.system.io.*
import xyz.angm.gamelin.system.io.ppu.CgbPPU
import xyz.angm.gamelin.system.io.ppu.Color
import xyz.angm.gamelin.system.io.ppu.DmgPPU
import xyz.angm.gamelin.system.io.ppu.GPUMode
import xyz.angm.gamelin.system.io.sound.*

/** Object responsible for creating and restoring save states, used
 * for both user save states and rewinding.
 * Rewinding is implemented by creating a save state into a byte buffer every other frame,
 * then playing it back by loading the save states in reverse. */
object SaveState {

    private const val BUFFER_SIZE = 85_000
    private const val BUFFER_PER_SEC = 30
    private const val MEGABYTES_PER_SEC = (BUFFER_PER_SEC * BUFFER_SIZE) / 1_000_000f
    val bufferSizeInMb get() = MEGABYTES_PER_SEC * config.rewindBufferSec

    private var rewindBuffers = Array(getBufferSize()) { ByteArray(BUFFER_SIZE) }
    private var bufferIdx = 0
    private val rewindOutput = Output()
    private val rewindInput = Input()
    private var rewindStopBuffer = 0

    /** Create a new save state for rewinding, called by [TileRenderer.finishFrame] */
    fun rewindPoint() {
        if (bufferIdx == rewindBuffers.size) bufferIdx = 0
        rewindOutput.buffer = rewindBuffers[bufferIdx++]
        kryo.writeObject(rewindOutput, gb)
    }

    /** Load the next rewind point when actively rewinding, will end rewind
     * if end of buffer is reached */
    fun rewindNext() {
        if (rewindStopBuffer == bufferIdx) endRewind()
        if (--bufferIdx < 0) bufferIdx = rewindBuffers.size - 1
        rewindInput.buffer = rewindBuffers[bufferIdx]
        loadState(rewindInput)
    }

    /** Stop rewinding and restore state */
    fun endRewind() {
        rewinding = false
        rewindStopBuffer = bufferIdx
        gb.debugger.emuHalt = false
    }

    /** Write a save state of the given GB to the given output. */
    fun saveState(out: Output, console: GameBoy) {
        kryo.writeObject(out, console)
        out.flush()
    }

    /** Load a state from the given input into the global [gb].
     * Since the serialized state does not contain native resources, they are
     * simply kept fron the previous console; the old [gb] is no longer
     * usable because of this. */
    fun loadState(input: Input) {
        val oldGb = gb

        val newGb = kryo.readObject(input, GameBoy::class.java)
        newGb.disposed = false // Can sometimes be true when loading the 'last' save state
        newGb.mmu.ppu.renderer = oldGb.mmu.ppu.renderer
        newGb.mmu.ppu.restore()
        newGb.mmu.cart.rom = oldGb.mmu.cart.rom
        newGb.debugger = oldGb.debugger
        gb = newGb

        // Don't actually call the dispose() method, all of the
        // native resources are carried to the new GB so it's fine
        oldGb.disposed = true
    }

    fun recreateBuffers() {
        rewindBuffers = Array(getBufferSize()) { ByteArray(BUFFER_SIZE) }
        bufferIdx = 0
    }

    private fun getBufferSize() = if (config.enableRewind) config.rewindBufferSec * BUFFER_PER_SEC else 0

    private val kryo = Kryo().apply {
        references = true
        instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

        val kls = arrayOf(
            ByteArray::class,
            GameBoy::class,
            CPU::class,
            MMU::class,
            Cartridge::class,
            NoMBC::class,
            MBC1::class,
            MBC2::class,
            MBC3::class,
            MBC3Timer::class,
            DateTime::class,
            MBC5::class,
            APU::class,
            AudioOutput::class,
            SquareWave1::class,
            SquareWave2::class,
            WaveChannel::class,
            NoiseChannel::class,
            LengthCounter::class,
            VolumeEnvelope::class,
            FrequencySweep::class,
            Lfsr::class,
            PolynomialCounter::class,
            Joypad::class,
            DmgPPU::class,
            CgbPPU::class,
            Color::class,
            GPUMode::class,
            Timer::class,
            DMA::class,
            HDMA::class,
            Array<Color>::class,
            Array<SoundChannel>::class,
            IntArray::class,
            BooleanArray::class,
        )
        for (k in kls) register(k.java)
    }
}