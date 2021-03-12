/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 4:20 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.system.DReg.*
import xyz.angm.gamelin.system.Flag.*
import xyz.angm.gamelin.system.Reg.*

open class Inst(val size: Short, val cycles: Int, val name: String, val incPC: Boolean = true, val execute: GameBoy.() -> Unit)

class BrInst(size: Short, cycles: Int, val cyclesWithBranch: Int, name: String, execute: GameBoy.() -> Boolean) :
    Inst(size, cycles, name, true, { execute() })

object InstSet {

    val op = arrayOfNulls<Inst>(256)
    val extOp = arrayOfNulls<Inst>(256)

    init {
        fillSet()
    }

    fun instOf(first: Int, second: Int): Inst {
        return when (first) {
            0xCB -> extOp[second]!!
            else -> op[first]!!
        }
    }
}

private fun fillSet() = InstSet.apply {
    val bdh = arrayOf(B, D, H)
    val cela = arrayOf(C, E, L, A)

    // -----------------------------------
    // 0x00 - 0x3F
    // -----------------------------------
    op[0x00] = Inst(1, 1, "NOP") { }
    op[0x10] = Inst(1, 1, "STOP") { throw InterruptedException("RIP") }
    op[0x20] = null
    op[0x30] = null

    DReg.values().forEachIndexed { i, r -> op[0x01 + (i shl 4)] = Inst(3, 3, "LD $r, d16") { write16(r, read16(cpu.pc + 1)) } }
    op[0x31] = Inst(3, 3, "LD SP, d16") { writeSP(read(cpu.pc + 1)) }

    op[0x02] = Inst(1, 2, "LD (BC), A") { write(read16(BC), read(A)) }
    op[0x12] = Inst(1, 2, "LD (DE), A") { write(read16(DE), read(A)) }
    op[0x22] = Inst(1, 2, "LD (HL+), A") { write(read16Modify(HL, +1), read(A)) }
    op[0x32] = Inst(1, 2, "LD (HL-), A") { write(read16Modify(HL, -1), read(A)) }

    DReg.values().forEachIndexed { i, r -> op[0x03 + (i shl 4)] = Inst(1, 2, "INC $r") { write16(r, read16(r) + 1) } }
    op[0x33] = Inst(1, 2, "INC SP") { writeSP(cpu.sp + 1) }

    bdh.forEachIndexed { i, r -> op[0x04 + (i shl 4)] = Inst(1, 1, "INC $r") { write(r, alu(read(r), 1, neg = 0)) } }
    op[0x34] = Inst(1, 3, "INC (HL)") { write(read16(HL), alu(read(read16(HL)), 1, neg = 0)) }

    bdh.forEachIndexed { i, r -> op[0x05 + (i shl 4)] = Inst(1, 1, "DEC $r") { write(r, alu(read(r), -1, neg = 1)) } }
    op[0x35] = Inst(1, 3, "DEC (HL)") { write(read16(HL), alu(read(read16(HL)), -1, neg = 1)) }

    bdh.forEachIndexed { i, r -> op[0x06 + (i shl 4)] = Inst(2, 2, "LD $r, d8") { write(r, read(cpu.pc + 1)) } }
    op[0x36] = Inst(1, 3, "LD (HL), d8") { write(read16(HL), read(cpu.pc + 1)) }

    op[0x07] = Inst(1, 1, "RLCA") {
        val a = read(A)
        write(A, (a shl 1) + (a ushr 7))
        write(F, Carry.from(a ushr 7))
    }
    op[0x17] = Inst(1, 1, "RLA") {
        val a = read(A)
        write(A, (a shl 1) + Carry.get(read(F)))
        write(F, Carry.from(a ushr 7))
    }
    op[0x27] = Inst(1, 1, "DAA") {
        var a = read(A)
        val f = read(F)
        if (!Negative.isSet(f)) {
            if (Carry.isSet(f) || a > 0x99) {
                a += 0x60
                cpu.flag(Carry, 1)
            }
            if (HalfCarry.isSet(f) || (a and 0x0f) > 0x09) a += 0x6
        } else {
            if (Carry.isSet(f)) a += 0x60
            if (HalfCarry.isSet(f)) a += 0x6
        }
        write(A, a)
        cpu.flag(Zero, if (a == 0) 1 else 0)
        cpu.flag(HalfCarry, 0)
    }
    op[0x37] = Inst(1, 1, "SCF") { write(F, (read(F) and Zero.mask) + 0b00010000) }

    op[0x08] = Inst(3, 5, "LD (a16), SP") {
        val addr = read16(cpu.pc + 1)
        write(addr, cpu.sp)
        write(addr + 1, cpu.sp.toInt() ushr 8)
    }
    op[0x18] = Inst(2, 3, "JR s8", incPC = false) { cpu.jmpRelative(read(cpu.pc + 1)) }
    op[0x28] = BrInst(2, 2, 3, "JR Z, s8") { cpu.brRelative(Zero.isSet(read(F)), read(cpu.pc + 1)) }
    op[0x38] = BrInst(2, 2, 3, "JR C, s8") { cpu.brRelative(Carry.isSet(read(F)), read(cpu.pc + 1)) }

    DReg.values().forEachIndexed { i, r -> op[0x09 + (i shl 4)] = Inst(1, 2, "ADD HL, $r") { write16(HL, alu16(read16(r), read16(HL), neg = 0)) } }
    op[0x39] = Inst(1, 2, "ADD HL, SP") { write16(HL, alu16(readSP(), read16(HL), neg = 0)) }

    op[0x0A] = Inst(1, 2, "LD A, (BC)") { write(A, read(read16(BC))) }
    op[0x1A] = Inst(1, 2, "LD A, (DE)") { write(A, read(read16(DE))) }
    op[0x2A] = Inst(1, 2, "LD A, (HL+)") { write(A, read(read16Modify(HL, +1))) }
    op[0x3A] = Inst(1, 2, "LD A, (HL-)") { write(A, read(read16Modify(HL, -1))) }

    DReg.values().forEachIndexed { i, r -> op[0x0B + (i shl 4)] = Inst(1, 2, "DEC $r") { write16(r, read16(r) - 1) } }
    op[0x3B] = Inst(1, 2, "DEC SP") { writeSP(readSP() - 1) }

    cela.forEachIndexed { i, r -> op[0x0C + (i shl 4)] = Inst(1, 1, "INC $r") { write(r, alu(read(r), 1, neg = 0)) } }
    cela.forEachIndexed { i, r -> op[0x0D + (i shl 4)] = Inst(1, 1, "DEC $r") { write(r, alu(read(r), -1, neg = 1)) } }
    cela.forEachIndexed { i, r -> op[0x0E + (i shl 4)] = Inst(2, 2, "LD $r, d8") { write(r, read(cpu.pc + 1)) } }

    op[0x0F] = Inst(1, 1, "RRCA") {
        val a = read(A)
        write(A, (a shr 1) + (a shl 7))
        write(F, Carry.from(a shl 7))
    }
    op[0x1F] = Inst(1, 1, "RRA") {
        val a = read(A)
        write(A, (a shr 1) + Carry.get(read(F)) shl 7)
        write(F, Carry.from(a and 1))
    }
    op[0x2F] = Inst(1, 1, "CPL") {
        cpu.flag(Negative, 1)
        cpu.flag(HalfCarry, 1)
        write(A, read(A) xor 0xFF)
    }
    op[0x3F] = Inst(1, 1, "CCF") {
        cpu.flag(Negative, 0)
        cpu.flag(HalfCarry, 0)
        cpu.flag(Carry, cpu.flagVal(Carry))
    }

    // -----------------------------------
    // 0x40 - 0x7F
    // -----------------------------------
    val regs = arrayOf(B, C, D, E, H, L)
    var idx = 0x40
    for (reg in regs) {
        for (from in regs) {
            op[idx++] = Inst(1, 1, "LD $reg, $from") { write(reg, read(from)) }
        }
        op[idx++] = Inst(1, 2, "LD $reg, (HL)") { write(reg, read(read16(HL))) }
        op[idx++] = Inst(1, 1, "LD $reg, A") { write(reg, read(A)) }
    }
    assert(idx == 0x70)

    for (from in regs) {
        op[idx++] = Inst(1, 2, "LD (HL), $from") { write(read16(HL), read(from)) }
    }
    op[idx++] = null // TODO: HALT
    op[idx++] = Inst(1, 2, "LD (HL), A") { write(read16(HL), read(A)) }
    for (from in regs) {
        op[idx++] = Inst(1, 1, "LD A, $from") { write(A, read(from)) }
    }
    op[idx++] = Inst(1, 1, "LD A, (HL)") { write(A, read(read16(HL))) }
    op[idx++] = Inst(1, 1, "LD A, A") { write(A, read(A)) }
    assert(idx == 0x80)

    // -----------------------------------
    // 0x80 - 0xCF
    // -----------------------------------
    val maths = arrayOf<Pair<String, GameBoy.(Int) -> Unit>>(
        "ADD" to { write(A, alu(read(A), it, neg = 0)) },
        "ADC" to { write(A, alu(read(A), it + Carry.get(read(F)), neg = 0)) }, // TODO: correct? idk about carry
        "SUB" to { write(A, alu(read(A), -it, neg = 1)) },
        "SBC" to { write(A, alu(read(A), -(it + Carry.get(read(F))), neg = 1)) }, // TODO: correct? idk about carry
        "AND" to {
            write(A, read(A) and it)
            write(F, Zero.from(read(A)) + HalfCarry.from(1))
        },
        "XOR" to {
            write(A, read(A) xor it)
            write(F, Zero.from(read(A)))
        },
        "OR" to {
            write(A, read(A) or it)
            write(F, Zero.from(read(A)))
        },
        "CP" to { alu(read(A), it, neg = 1) },
    )

    for (kind in maths) {
        val name = kind.first
        val exec = kind.second

        for (from in regs) {
            op[idx++] = Inst(1, 1, "$name A, $from") { exec(this, read(from)) }
        }
        op[idx++] = Inst(1, 2, "$name A, (HL)") { exec(this, read(read16(HL))) }
        op[idx++] = Inst(1, 1, "$name A, A") { exec(this, read(A)) }
    }
}

