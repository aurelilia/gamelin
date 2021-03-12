/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 1:38 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import mu.KotlinLogging

class GameBoy {

    internal val cpu = Cpu(this)

    fun advance() {
        val inst = cpu.nextInstruction()
        println("Took ${inst.cycles} for instruction ${inst.name} at 0x${cpu.pc.toString(16)}")
    }

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
    internal fun read(reg: DReg) = (read(reg.low) + (read(reg.high) shl 8))

    internal fun read16(addr: Short): Int {
        val ls = read(addr)
        val hs = read(addr + 1)
        return ((hs shl 8) or ls)
    }

    internal fun read16(addr: Int) = read16(addr.toShort())

    /** Read the given register and return it's value;
     * also write (value + mod) to the register. */
    internal fun readModify(reg: DReg, mod: Short): Short {
        val ret = (read(reg.low) + (read(reg.high) shl 8))
        write(reg, ret + mod)
        return ret.toShort()
    }

    internal fun write(addr: Short, value: Byte): GameBoy {
        when (addr) {
            else -> log.warn { "Encountered write to invalid address 0x${addr.toString(16)}, value 0x${value}" }
        }
        return this
    }

    internal fun write(addr: Int, value: Int) = write(addr.toShort(), value.toByte())
    internal fun write(addr: Short, value: Int) = write(addr, value.toByte())

    internal fun write(reg: Reg, value: Byte) {
        cpu.regs[reg.idx] = value
    }

    internal fun write(reg: Reg, value: Int) = write(reg, value.toByte())

    internal fun write(reg: DReg, value: Int) {
        write(reg.high, (value ushr 8).toByte())
        write(reg.low, value.toByte())
    }

    internal fun writeSP(value: Int) {
        cpu.sp = value.toShort()
    }

    internal fun alu(a: Int, b: Int, neg: Int): Int {
        val result = a + b
        cpu.flag(Flag.Zero, if (result == 0) 1 else 0)
        cpu.flag(Flag.Negative, neg)
        cpu.flag(Flag.HalfCarry, ((a and 0xf) + (b and 0xf) and 0x10))
        return result
    }

    internal fun alu16(a: Int, b: Int, neg: Int): Int {
        return alu(a and 0xFF, b and 0xFF, neg) + alu(a and 0xFF00, b and 0xFF00, neg)
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}