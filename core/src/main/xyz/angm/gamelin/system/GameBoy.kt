/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 8:55 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import com.badlogic.gdx.utils.Disposable
import mu.KotlinLogging
import xyz.angm.gamelin.*
import xyz.angm.gamelin.interfaces.Debugger
import xyz.angm.gamelin.interfaces.Keyboard
import xyz.angm.gamelin.system.sound.Sound
import kotlin.experimental.and

const val CLOCK_SPEED_HZ = 4194304

class GameBoy(internal val debugger: Debugger = Debugger()) : Disposable {

    internal val cpu = CPU(this)
    internal val ppu = PPU(this)
    internal val joypad = Keyboard(this)
    internal val sound = Sound(this)
    internal val mmu = MMU(this)
    private val timer = Timer(this)
    private var clock = 0

    constructor(game: ByteArray, debugger: Debugger) : this(debugger) {
        loadGame(game)
    }

    fun loadGame(game: ByteArray) {
        mmu.load(game)
        reset()
        debugger.emuHalt = false
    }

    fun advanceDelta(delta: Float) {
        if (debugger.emuHalt) return
        val target = clock + (if (debugger.slow) 1 else (CLOCK_SPEED_HZ * delta).toInt())
        while (clock < target && !debugger.emuHalt) advance()
    }

    fun advance() {
        debugger.preAdvance(this)
        cpu.nextInstruction()
        debugger.postAdvance(this)
    }

    fun getNextInst(): Inst {
        val inst = InstSet.instOf(read(cpu.pc), read(cpu.pc + 1))
        return if (inst == null) {
            debugger.emuHalt = true
            InstSet.instOf(0x0, 0x0)!! // NOP
        } else inst
    }

    fun reset() {
        cpu.reset()
        ppu.reset()
        joypad.reset()
        timer.reset()
        clock = 0
    }

    // -----------------------------------
    // Timing
    // -----------------------------------
    internal fun advanceClock(mCycles: Int) {
        val tCycles = mCycles * 4
        ppu.step(tCycles)
        timer.step(mCycles)
        sound.step(tCycles)
        clock += tCycles
    }

    // -----------------------------------
    // Reading of memory/values
    // -----------------------------------
    internal fun read(addr: Short) = mmu.read(addr).int()
    internal fun read(addr: Int) = read(addr.toShort())
    internal fun read(reg: Reg) = cpu.regs[reg.idx].int()
    private fun readSigned(addr: Int) = mmu.read(addr.toShort()).toInt()
    internal fun read16(reg: DReg) = (read(reg.low) or (read(reg.high) shl 8))
    internal fun readAny(addr: Int) = mmu.readAny(addr.toShort()).int()

    private fun read16(addr: Short): Int {
        val ls = read(addr)
        val hs = read(addr + 1)
        return ((hs shl 8) or ls)
    }

    internal fun read16(addr: Int) = read16(addr.toShort())
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
    internal fun writeAny(addr: Int, value: Int) = mmu.writeAny(addr.toShort(), value.toByte())

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
    // Math/ALU
    // -----------------------------------
    // c is the value of the carry, only used by ADC
    fun add(a: Int, b: Int, c: Int = 0, setCarry: Boolean = false): Int {
        val result = a + b + c
        cpu.flag(Flag.Zero, if ((result and 0xFF) == 0) 1 else 0)
        cpu.flag(Flag.Negative, 0)
        cpu.flag(Flag.HalfCarry, ((a and 0xF) + (b and 0xF) + c and 0x10))
        if (setCarry) cpu.flag(Flag.Carry, (a and 0xFF) + (b and 0xFF) + c and 0x100)
        return result.toByte().int()
    }

    // c is the value of the carry, only used by SBC
    fun sub(a: Int, b: Int, c: Int = 0, setCarry: Boolean = false): Int {
        val result = a - b - c
        cpu.flag(Flag.Zero, if ((result and 0xFF) == 0) 1 else 0)
        cpu.flag(Flag.Negative, 1)
        cpu.flag(Flag.HalfCarry, ((a and 0xF) - (b and 0xF) - c and 0x10))
        if (setCarry) cpu.flag(Flag.Carry, (a and 0xFF) - (b and 0xFF) - c and 0x100)
        return result.toByte().int()
    }

    fun add16HL(other: Int) {
        val hl = read16(DReg.HL)
        val result = hl + other
        cpu.flag(Flag.Negative, 0)
        cpu.flag(Flag.HalfCarry, (hl and 0xFFF) + (other and 0xFFF) and 0x1000)
        cpu.flag(Flag.Carry, (hl and 0xFFFF) + (other and 0xFFFF) and 0x10000)
        write16(DReg.HL, result)
    }

    fun modRetHL(mod: Short): Short {
        val ret = read16(DReg.HL)
        write16(DReg.HL, ret + mod)
        return ret.toShort()
    }

    // Thanks to https://stackoverflow.com/questions/5159603/gbz80-how-does-ld-hl-spe-affect-h-and-c-flags
    // as well as kotcrab's xgbc emulator for showing me correct behavior for 0xE8 and 0xF8!
    internal fun addSP(): Int {
        val value = readSigned(cpu.pc + 1)
        cpu.flag(Flag.Zero, 0)
        cpu.flag(Flag.Negative, 0)
        cpu.flag(Flag.HalfCarry, (cpu.sp and 0xF) + (value and 0xF) and 0x10)
        cpu.flag(Flag.Carry, (cpu.sp and 0xFF) + (value and 0xFF) and 0x100)
        return cpu.sp + value
    }

    fun rlc(value: Byte, maybeSetZ: Boolean): Byte {
        val result = value.rotLeft(1)
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (maybeSetZ && result == 0.toByte()) Flag.Zero.mask else 0)
        return result
    }

    fun rrc(value: Byte, maybeSetZ: Boolean): Byte {
        val result = value.rotRight(1)
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (maybeSetZ && result == 0.toByte()) Flag.Zero.mask else 0)
        return result
    }

    fun rl(value: Byte, maybeSetZ: Boolean): Byte {
        val result = value.rotLeft(1).setBit(0, cpu.flagVal(Flag.Carry))
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun rr(value: Byte, maybeSetZ: Boolean): Byte {
        val result = value.rotRight(1).setBit(7, cpu.flagVal(Flag.Carry))
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun sla(value: Byte): Byte {
        val result = (value.int() shl 1) and 0xFF
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun sra(value: Byte): Byte {
        val a = ((value.int() ushr 1) and 0xFF).toByte()
        val result = a.setBit(7, value.isBit(7))
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun swap(value: Byte): Byte {
        val upper = value.int() shr 4
        val lower = (value.int() and 0xF) shl 4
        write(Reg.F, if ((upper + lower) == 0) Flag.Zero.mask else 0)
        return (lower + upper).toByte()
    }

    fun srl(value: Byte): Byte {
        val result = (value.int() shr 1) and 0xFF
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun bit(value: Byte, bit: Int): Byte {
        cpu.flag(Flag.Zero, value.isBit(bit) xor 1)
        cpu.flag(Flag.Negative, 0)
        cpu.flag(Flag.HalfCarry, 1)
        return value
    }

    fun zFlagOnly(value: Int) {
        if (value == 0) write(Reg.F, Flag.Zero.mask)
        else write(Reg.F, 0)
    }

    // -----------------------------------
    // Stack Pointer operations
    // -----------------------------------
    fun popS(): Int {
        val value = read16(cpu.sp)
        cpu.sp = (cpu.sp + 2).toShort()
        return value
    }

    fun pushS(value: Int) {
        cpu.sp = (cpu.sp - 2).toShort()
        write16(cpu.sp, value)
    }

    // -----------------------------------
    // Control Flow
    // -----------------------------------
    fun jr(): Boolean {
        // All JR instructions are 2 bytes long
        cpu.pc = (cpu.pc + read(cpu.pc + 1).toByte() + 2).toShort()
        return true
    }

    fun jp(): Boolean {
        cpu.pc = read16(cpu.pc + 1).toShort()
        return true
    }

    fun call(): Boolean {
        pushS(cpu.pc + 3) // Call opcodes are 3 bytes long
        return jp()
    }

    fun ret(): Boolean {
        cpu.pc = popS().toShort()
        return true
    }

    // -----------------------------------
    // Interrupts
    // -----------------------------------
    internal fun requestInterrupt(interrupt: Interrupt) {
        write(Interrupt.IF, read(Interrupt.IF).toByte().setBit(interrupt.position, 1))
    }

    override fun dispose() {
        ppu.dispose()
        debugger.dispose()
    }

    internal companion object {
        val log = KotlinLogging.logger { }
    }
}