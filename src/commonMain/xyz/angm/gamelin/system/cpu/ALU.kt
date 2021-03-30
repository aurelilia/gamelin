/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:18 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.cpu

import xyz.angm.gamelin.*
import xyz.angm.gamelin.system.GameBoy
import kotlin.experimental.and

// This file contains a bunch of math operations that are out of scope for
// the GameBoy file itself.

// c is the value of the carry, only used by ADC
internal fun GameBoy.add(a: Int, b: Int, c: Int = 0, setCarry: Boolean = false): Int {
    val result = a + b + c
    cpu.flag(Flag.Zero, (result and 0xFF) == 0)
    cpu.flag(Flag.Negative, false)
    cpu.flag(Flag.HalfCarry, ((a and 0xF) + (b and 0xF) + c and 0x10))
    if (setCarry) cpu.flag(Flag.Carry, ((a and 0xFF) + (b and 0xFF) + c and 0x100))
    return result.toByte().int()
}

// c is the value of the carry, only used by SBC
internal fun GameBoy.sub(a: Int, b: Int, c: Int = 0, setCarry: Boolean = false): Int {
    val result = a - b - c
    cpu.flag(Flag.Zero, (result and 0xFF) == 0)
    cpu.flag(Flag.Negative, true)
    cpu.flag(Flag.HalfCarry, ((a and 0xF) - (b and 0xF) - c and 0x10))
    if (setCarry) cpu.flag(Flag.Carry, (a and 0xFF) - (b and 0xFF) - c and 0x100)
    return result.toByte().int()
}

internal fun GameBoy.add16HL(other: Int) {
    val hl = read16(DReg.HL)
    val result = hl + other
    cpu.flag(Flag.Negative, false)
    cpu.flag(Flag.HalfCarry, (hl and 0xFFF) + (other and 0xFFF) and 0x1000)
    cpu.flag(Flag.Carry, (hl and 0xFFFF) + (other and 0xFFFF) and 0x10000)
    write16(DReg.HL, result)
}

internal fun GameBoy.modRetHL(mod: Short): Short {
    val ret = read16(DReg.HL)
    write16(DReg.HL, ret + mod)
    return ret.toShort()
}

// Thanks to https://stackoverflow.com/questions/5159603/gbz80-how-does-ld-hl-spe-affect-h-and-c-flags
// as well as kotcrab's xgbc emulator for showing me correct behavior for 0xE8 and 0xF8!
internal fun GameBoy.addSP(): Int {
    val value = readSigned(cpu.pc + 1)
    cpu.flag(Flag.Zero, false)
    cpu.flag(Flag.Negative, false)
    cpu.flag(Flag.HalfCarry, (cpu.sp and 0xF) + (value and 0xF) and 0x10)
    cpu.flag(Flag.Carry, (cpu.sp and 0xFF) + (value and 0xFF) and 0x100)
    return cpu.sp + value
}

internal fun GameBoy.rlc(value: Byte, maybeSetZ: Boolean): Byte {
    val result = value.rotLeft(1)
    write(Reg.F, Flag.Carry.from(value.bit(7)) + if (maybeSetZ && result == 0.toByte()) Flag.Zero.mask else 0)
    return result
}

internal fun GameBoy.rrc(value: Byte, maybeSetZ: Boolean): Byte {
    val result = value.rotRight(1)
    write(Reg.F, Flag.Carry.from(value.bit(0)) + if (maybeSetZ && result == 0.toByte()) Flag.Zero.mask else 0)
    return result
}

internal fun GameBoy.rl(value: Byte, maybeSetZ: Boolean): Byte {
    val result = value.rotLeft(1).setBit(0, cpu.flagVal(Flag.Carry))
    write(Reg.F, Flag.Carry.from(value.bit(7)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
    return result.toByte()
}

internal fun GameBoy.rr(value: Byte, maybeSetZ: Boolean): Byte {
    val result = value.rotRight(1).setBit(7, cpu.flagVal(Flag.Carry))
    write(Reg.F, Flag.Carry.from(value.bit(0)) + if (maybeSetZ && result == 0) Flag.Zero.mask else 0)
    return result.toByte()
}

internal fun GameBoy.sla(value: Byte): Byte {
    val result = (value.int() shl 1) and 0xFF
    write(Reg.F, Flag.Carry.from(value.bit(7)) + if (result == 0) Flag.Zero.mask else 0)
    return result.toByte()
}

internal fun GameBoy.sra(value: Byte): Byte {
    val a = ((value.int() ushr 1) and 0xFF).toByte()
    val result = a.setBit(7, value.bit(7))
    write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) Flag.Zero.mask else 0)
    return result.toByte()
}

internal fun GameBoy.swap(value: Byte): Byte {
    val upper = value.int() shr 4
    val lower = (value.int() and 0xF) shl 4
    write(Reg.F, if ((upper + lower) == 0) Flag.Zero.mask else 0)
    return (lower + upper).toByte()
}

internal fun GameBoy.srl(value: Byte): Byte {
    val result = (value.int() shr 1) and 0xFF
    write(Reg.F, Flag.Carry.from(value.bit(0)) + if (result == 0) Flag.Zero.mask else 0)
    return result.toByte()
}

internal fun GameBoy.bit(value: Byte, bit: Int): Byte {
    cpu.flag(Flag.Zero, value.bit(bit) xor 1)
    cpu.flag(Flag.Negative, false)
    cpu.flag(Flag.HalfCarry, true)
    return value
}

internal fun GameBoy.zFlagOnly(value: Int) {
    if (value == 0) write(Reg.F, Flag.Zero.mask)
    else write(Reg.F, 0)
}
