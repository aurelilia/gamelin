/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 4:49 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import mu.KotlinLogging
import xyz.angm.gamelin.bit
import xyz.angm.gamelin.int
import xyz.angm.gamelin.rotLeft
import xyz.angm.gamelin.rotRight

class GameBoy(game: ByteArray) {

    internal val cpu = CPU(this)
    internal val gpu = GPU(this)
    internal val mmu = MMU(this, game)
    internal var clock = 0

    fun advance(force: Boolean = false) {
        if (cpu.halt && !force) return
        val pc = cpu.pc
        val inst = cpu.nextInstruction()
        clock += inst.cycles
        // println("Took ${inst.cycles} for instruction ${inst.name} at ${pc.hex16()}")
    }

    fun getNextInst() = InstSet.instOf(read(cpu.pc), read(cpu.pc + 1))!!

    // -----------------------------------
    // Reading of memory/values
    // -----------------------------------
    internal fun read(addr: Short) = mmu.read(addr).int() and 0xFF
    internal fun read(addr: Int) = read(addr.toShort())
    internal fun read(reg: Reg) = cpu.regs[reg.idx].int()
    internal fun read16(reg: DReg) = (read(reg.low) or (read(reg.high) shl 8))

    internal fun read16(addr: Short): Int {
        val ls = read(addr)
        val hs = read(addr + 1)
        return ((hs shl 8) or ls)
    }

    internal fun read16(addr: Int) = read16(addr.toShort())

    internal fun readSP() = cpu.sp.int()

    /** Read the given d-register and return it's value;
     * also write (value + mod) to the register. */
    internal fun read16Modify(reg: DReg, mod: Short): Short {
        val ret = (read(reg.low) + (read(reg.high) shl 8))
        write16(reg, ret + mod)
        return ret.toShort()
    }

    // -----------------------------------
    // Writing of memory/values
    // -----------------------------------
    internal fun write(addr: Short, value: Byte) = mmu.write(addr, value)
    internal fun write(addr: Int, value: Int) = write(addr.toShort(), value.toByte())
    internal fun write(addr: Short, value: Int) = write(addr, value.toByte())
    internal fun write(addr: Int, value: Short) = write(addr.toShort(), value.toByte())
    internal fun write(addr: Int, value: Byte) = write(addr.toShort(), value)

    internal fun write(reg: Reg, value: Byte) {
        cpu.regs[reg.idx] = value
    }

    internal fun write(reg: Reg, value: Int) = write(reg, value.toByte())

    internal fun write16(reg: DReg, value: Int) {
        write(reg.high, (value ushr 8).toByte())
        write(reg.low, value.toByte())
    }

    internal fun write16(location: Short, value: Int) {
        write(location, value)
        write(location + 1, value ushr 8)
    }

    internal fun writeSP(value: Int) {
        cpu.sp = value.toShort()
    }

    // -----------------------------------
    // Math/ALU
    // -----------------------------------
    internal fun alu(a: Int, b: Int, neg: Int): Int {
        val result = a + b
        val truncResult = result.toByte()
        cpu.flag(Flag.Zero, if (truncResult == 0.toByte()) 1 else 0)
        cpu.flag(Flag.Negative, neg)
        cpu.flag(Flag.HalfCarry, ((a and 0xF) + (b and 0xF) and 0x10))
        cpu.flag(Flag.Carry, if (result > 255 || result < 0) 1 else 0)
        return truncResult.int()
    }

    internal fun add16HL(other: Int) {
        val hl = read16(DReg.HL)
        val result = hl + other
        cpu.flag(Flag.Negative, 0)
        cpu.flag(Flag.HalfCarry, (hl and 0xFFF) + (other and 0xFFF) and 0x1000)
        cpu.flag(Flag.Carry, (hl and 0xFFFF) + (other and 0xFFFF) and 0x10000)
        write16(DReg.HL, result)
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
        val result = value.rotLeft(1) + cpu.flagVal(Flag.Carry)
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun rr(value: Byte, maybeSetZ: Boolean): Byte {
        val result = value.rotRight(1) + (cpu.flagVal(Flag.Carry) shl 7)
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun sla(value: Byte): Byte {
        val result = (value.int() and 0xFF) shl 1
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun sra(value: Byte): Byte {
        val result = ((((value.int() and 0xFF) ushr 1) and 0x7F).toByte() + value) and 0x80
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
        val result = (value.int() and 0xFF) shr 1
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) 1 else 0)
        return result.toByte()
    }

    fun bit(value: Byte, bit: Int): Byte {
        val value = (value.int() and (1 shl bit)) shr bit
        write(Reg.F, if (cpu.flag(Flag.Carry)) Flag.Carry.mask else 0 + Flag.HalfCarry.mask + if (value == 0) Flag.Zero.mask else 0)
        return value.toByte()
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

    companion object {
        private val log = KotlinLogging.logger { }
    }
}