/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:22 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.*
import xyz.angm.gamelin.interfaces.AudioOutput
import xyz.angm.gamelin.interfaces.Debugger
import xyz.angm.gamelin.system.cpu.*
import xyz.angm.gamelin.system.io.MMU
import kotlin.experimental.and
import kotlin.jvm.Transient

/** The entire GB system.
 * @property gameLoaded If a game has been loaded and the system is ready to be emulated/used
 * @property disposed If the system is disposed and should cease all activities.
 * @property cgbMode If the system is running in CGB mode instead of DMG */
class GameBoy(@Transient var debugger: Debugger = Debugger()) : Disposable {

    internal val cpu = CPU(this)
    val mmu = MMU(this)
    var gameLoaded = false
    var disposed = false
    var cgbMode = false
    var tSpeedMultiplier = 1
    internal var clock = 0

    constructor(game: ByteArray, debugger: Debugger) : this(debugger) {
        loadGame(game)
    }

    /** Load a game and reset the system.
     * Will set [Debugger.emuHalt] to `false`. */
    fun loadGame(game: ByteArray) {
        mmu.load(game)
        gameLoaded = true
        reset()
        debugger.emuHalt = false
    }

    /** Advance the system by at least the given delta.
     * Might advance a few dozen T-cycles more due to the non-deterministic nature of the CPU,
     * or even hundreds more should a GDMA transfer occur at the wrong time (which will be finished immediately). */
    fun advanceDelta(delta: Float) {
        clock = 0
        val target = (T_CLOCK_HZ * delta)
        while (clock < target) advance()
    }

    /** Advance the system forever until it's disposed.
     * For this to work, some other timing mechanism needs to be used
     * that halts execution; on the desktop, this is done by making
     * [AudioOutput.play] block until almost all samples have been played.
     * @param sleep Called when [Debugger.emuHalt] is true. */
    inline fun advanceIndefinitely(sleep: () -> Unit) {
        while (!disposed) {
            if (debugger.emuHalt) sleep()
            else advance()
        }
    }

    /** Advance until the debugger halts the system. */
    fun advanceUntilDebugHalt() {
        while (!debugger.emuHalt) advance()
    }

    /** Advance the system by a single CPU instruction.
     * Due to GDMA/HDMA transfers, this can sometimes advance hundreds of t-cycles! */
    fun advance() {
        debugger.preAdvance(this)
        cpu.nextInstruction()
        debugger.postAdvance(this)
    }

    /** Skip the boot rom, and start the game immediately.
     * Not supported in CGB mode, will do nothing. */
    fun skipBootRom() {
        if (cgbMode) return
        cpu.pc = 0x100
        mmu.bootromOn = false
    }

    /** Returns the next instruction the CPU will execute.
     * Will stop the system and return NOP if it is an unknown opcode. */
    fun getNextInst(): Inst {
        val inst = InstSet.instOf(read(cpu.pc), read(cpu.pc + 1))
        return if (inst == null) {
            debugger.emuHalt = true
            InstSet.instOf(0x0, 0x0)!! // NOP
        } else inst
    }

    fun reset() {
        if (!gameLoaded) return
        cgbMode = mmu.cart.requiresCGB || (mmu.cart.supportsCGB && configuration.preferCGB)
        cpu.reset()
        mmu.reset()
        clock = 0
        tSpeedMultiplier = 1
    }

    // -----------------------------------
    // Timing
    // -----------------------------------
    internal fun advanceClock(mCycles: Int) {
        val tCycles = mCycles * 4
        mmu.step(tCycles)
        clock += tCycles
    }

    // -----------------------------------
    // Reading of memory/values
    // -----------------------------------
    internal fun read(addr: Short) = mmu.read(addr).int()
    internal fun read(addr: Int) = read(addr.toShort())
    internal fun read(reg: Reg) = cpu.regs[reg.ordinal].int()
    internal fun readSigned(addr: Int) = mmu.read(addr.toShort()).toInt()
    internal fun read16(reg: DReg) = (read(reg.low) or (read(reg.high) shl 8))

    internal fun read16(addr: Int) = mmu.read16(addr)
    internal fun readSP() = cpu.sp.int()

    // -----------------------------------
    // Writing of memory/values
    // -----------------------------------
    private fun write(addr: Short, value: Byte) = mmu.write(addr, value)
    internal fun write(addr: Int, value: Int) = write(addr.toShort(), value.toByte())
    internal fun write(addr: Short, value: Int) = write(addr, value.toByte())
    internal fun write(addr: Int, value: Short) = write(addr.toShort(), value.toByte())
    internal fun write(addr: Int, value: Byte) = write(addr.toShort(), value)
    internal fun write(reg: Reg, value: Byte) = cpu.write(reg, value)
    internal fun write(reg: Reg, value: Int) = write(reg, value.toByte())

    internal fun write16(reg: DReg, value: Int) {
        write(reg.high, (value ushr 8).toByte())
        write(reg.low, value.toByte())
    }

    private fun write16(location: Short, value: Int) {
        write(location, value)
        write(location + 1, value ushr 8)
    }

    internal fun writeSP(value: Int) {
        cpu.sp = value.toShort()
    }

    // -----------------------------------
    // Stack Pointer operations
    // -----------------------------------
    internal fun popS(): Int {
        val value = read16(cpu.sp.int())
        cpu.sp = (cpu.sp + 2).toShort()
        return value
    }

    internal fun pushS(value: Int) {
        cpu.sp = (cpu.sp - 2).toShort()
        write16(cpu.sp, value)
    }

    // -----------------------------------
    // Control Flow
    // -----------------------------------
    internal fun jr(): Boolean {
        // All JR instructions are 2 bytes long
        cpu.pc = (cpu.pc + read(cpu.pc + 1).toByte() + 2).toShort()
        return true
    }

    internal fun jp(): Boolean {
        cpu.pc = read16(cpu.pc + 1).toShort()
        return true
    }

    internal fun call(): Boolean {
        pushS(cpu.pc + 3) // Call opcodes are 3 bytes long
        return jp()
    }

    internal fun ret(): Boolean {
        cpu.pc = popS().toShort()
        return true
    }

    override fun dispose() {
        disposed = true
        mmu.dispose()
        debugger.dispose()
    }

    companion object {
        /** The clock speed of the GB's internal clock ("T-cycles") in HZ.
         * CGB double speed mode is twice as fast. */
        const val T_CLOCK_HZ = 4194304
    }
}