/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 6:32 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import mu.KotlinLogging
import xyz.angm.gamelin.bit
import xyz.angm.gamelin.rotLeft
import xyz.angm.gamelin.rotRight

class GameBoy {

    internal val cpu = Cpu(this)

    fun advance() {
        val inst = cpu.nextInstruction()
        println("Took ${inst.cycles} for instruction ${inst.name} at 0x${cpu.pc.toString(16)}")
    }

    // -----------------------------------
    // Reading of memory/values
    // -----------------------------------
    internal fun read(addr: Short): Int {
        return when (addr) {
            else -> {
                log.warn { "Encountered read from invalid address 0x${addr.toString(16)}, returning 0x0" }
                0
            }
        }
    }

    internal fun read(addr: Int) = read(addr.toShort())
    internal fun read(reg: Reg) = cpu.regs[reg.idx].toInt()
    internal fun read16(reg: DReg) = (read(reg.low) + (read(reg.high) shl 8))

    internal fun read16(addr: Short): Int {
        val ls = read(addr)
        val hs = read(addr + 1)
        return ((hs shl 8) or ls)
    }

    internal fun read16(addr: Int) = read16(addr.toShort())

    internal fun readSP() = cpu.sp.toInt()

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
    internal fun write(addr: Short, value: Byte): GameBoy {
        when (addr) {
            else -> log.warn { "Encountered write to invalid address 0x${addr.toString(16)}, value 0x${value}" }
        }
        return this
    }

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
        cpu.flag(Flag.HalfCarry, ((a and 0xf) + (b and 0xf) and 0x10))
        cpu.flag(Flag.Carry, if (result > 255 || result < 0) 1 else 0)
        return truncResult.toInt()
    }

    internal fun alu16(a: Int, b: Int, neg: Int): Int {
        // Zero flag does NOT get affected by 16-bit math for some reason, thanks Sharp
        val zero = cpu.flagVal(Flag.Zero)
        val res = alu(a and 0xFF, b and 0xFF, neg) + alu(a and 0xFF00, b and 0xFF00, neg)
        cpu.flag(Flag.Zero, zero)
        return res
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
        val result = (value.toInt() and 0xFF) shl 1
        write(Reg.F, Flag.Carry.from(value.bit(7)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun sra(value: Byte): Byte {
        val result = ((((value.toInt() and 0xFF) ushr 1) and 0x7F).toByte() + value) and 0x80
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) Flag.Zero.mask else 0)
        return result.toByte()
    }

    fun swap(value: Byte): Byte {
        val upper = value.toInt() shr 4
        val lower = (value.toInt() and 0xF) shl 4
        write(Reg.F, if ((upper + lower) == 0) Flag.Zero.mask else 0)
        return (lower + upper).toByte()
    }

    fun srl(value: Byte): Byte {
        val result = (value.toInt() and 0xFF) shr 1
        write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) 1 else 0)
        return result.toByte()
    }

    fun bit(value: Byte, bit: Int): Byte {
        val value = (value.toInt() and (1 shl bit)) shr bit
        write(Reg.F, if (cpu.flag(Flag.Carry)) Flag.Carry.mask else 0 + Flag.HalfCarry.mask + if (value == 0) Flag.Zero.mask else 0)
        return value.toByte()
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
    fun call(): Boolean {
        pushS(cpu.pc + 3) // Call opcodes are 3 bytes long
        cpu.pc = read16(cpu.pc + 1).toShort()
        return true
    }

    fun ret(): Boolean {
        cpu.pc = popS().toShort()
        return true
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}