/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 1:48 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.DReg.*
import xyz.angm.gamelin.system.Flag.*
import xyz.angm.gamelin.system.Reg.*

open class Inst(val size: Short, val cycles: Int, val name: String, val incPC: Boolean = true, val execute: GameBoy.() -> Unit)

class BrInst(size: Short, cycles: Int, val cyclesWithBranch: Int, name: String, val executeBr: GameBoy.() -> Boolean) :
    Inst(size, cycles, name, true, { executeBr() })

object InstSet {

    val op = arrayOfNulls<Inst>(256)
    val ep = arrayOfNulls<Inst>(256)

    init {
        fillSet()
        fillExt()
    }

    fun instOf(first: Int, second: Int): Inst? {
        return when (first) {
            0xCB -> ep[second]
            else -> op[first]
        }
    }
}

private fun fillSet() = InstSet.apply {
    val bcdehl = arrayOf(BC, DE, HL)
    val bdh = arrayOf(B, D, H)
    val cela = arrayOf(C, E, L, A)

    // -----------------------------------
    // 0x00 - 0x3F
    // -----------------------------------
    op[0x00] = Inst(1, 1, "NOP") { }
    op[0x10] = Inst(1, 1, "STOP") { throw InterruptedException("RIP") }
    op[0x20] = BrInst(2, 2, 3, "JR NZ, s8") { if (!cpu.flag(Zero)) jr() else false }
    op[0x30] = BrInst(2, 2, 3, "JR NC, s8") { if (!cpu.flag(Carry)) jr() else false }

    bcdehl.forEachIndexed { i, r -> op[0x01 + (i shl 4)] = Inst(3, 3, "LD $r, d16") { write16(r, read16(cpu.pc + 1)) } }
    op[0x31] = Inst(3, 3, "LD SP, d16") { writeSP(read16(cpu.pc + 1)) }

    op[0x02] = Inst(1, 2, "LD (BC), A") { write(read16(BC), read(A)) }
    op[0x12] = Inst(1, 2, "LD (DE), A") { write(read16(DE), read(A)) }
    op[0x22] = Inst(1, 2, "LD (HL+), A") { write(read16Modify(HL, +1), read(A)) }
    op[0x32] = Inst(1, 2, "LD (HL-), A") { write(read16Modify(HL, -1), read(A)) }

    bcdehl.forEachIndexed { i, r -> op[0x03 + (i shl 4)] = Inst(1, 2, "INC $r") { write16(r, read16(r) + 1) } }
    op[0x33] = Inst(1, 2, "INC SP") { writeSP(cpu.sp + 1) }

    bdh.forEachIndexed { i, r -> op[0x04 + (i shl 4)] = Inst(1, 1, "INC $r") { write(r, alu(read(r), 1, neg = 0)) } }
    op[0x34] = Inst(1, 3, "INC (HL)") { write(read16(HL), alu(read(read16(HL)), 1, neg = 0)) }

    bdh.forEachIndexed { i, r -> op[0x05 + (i shl 4)] = Inst(1, 1, "DEC $r") { write(r, alu(read(r), -1, neg = 1)) } }
    op[0x35] = Inst(1, 3, "DEC (HL)") { write(read16(HL), alu(read(read16(HL)), -1, neg = 1)) }

    bdh.forEachIndexed { i, r -> op[0x06 + (i shl 4)] = Inst(2, 2, "LD $r, d8") { write(r, read(cpu.pc + 1)) } }
    op[0x36] = Inst(1, 3, "LD (HL), d8") { write(read16(HL), read(cpu.pc + 1)) }

    op[0x07] = Inst(1, 1, "RLCA") { write(A, rlc(read(A).toByte(), false)) }
    op[0x17] = Inst(1, 1, "RLA") { write(A, rl(read(A).toByte(), false)) }
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
        write(addr + 1, cpu.sp.int() ushr 8)
    }
    op[0x18] = Inst(2, 3, "JR s8", incPC = false) { jr() }
    op[0x28] = BrInst(2, 2, 3, "JR Z, s8") { if (cpu.flag(Zero)) jr() else false }
    op[0x38] = BrInst(2, 2, 3, "JR C, s8") { if (cpu.flag(Carry)) jr() else false }

    bcdehl.forEachIndexed { i, r -> op[0x09 + (i shl 4)] = Inst(1, 2, "ADD HL, $r") { write16(HL, alu16(read16(r), read16(HL), neg = 0)) } }
    op[0x39] = Inst(1, 2, "ADD HL, SP") { write16(HL, alu16(readSP(), read16(HL), neg = 0)) }

    op[0x0A] = Inst(1, 2, "LD A, (BC)") { write(A, read(read16(BC))) }
    op[0x1A] = Inst(1, 2, "LD A, (DE)") { write(A, read(read16(DE))) }
    op[0x2A] = Inst(1, 2, "LD A, (HL+)") { write(A, read(read16Modify(HL, +1))) }
    op[0x3A] = Inst(1, 2, "LD A, (HL-)") { write(A, read(read16Modify(HL, -1))) }

    bcdehl.forEachIndexed { i, r -> op[0x0B + (i shl 4)] = Inst(1, 2, "DEC $r") { write16(r, read16(r) - 1) } }
    op[0x3B] = Inst(1, 2, "DEC SP") { writeSP(readSP() - 1) }

    cela.forEachIndexed { i, r -> op[0x0C + (i shl 4)] = Inst(1, 1, "INC $r") { write(r, alu(read(r), 1, neg = 0)) } }
    cela.forEachIndexed { i, r -> op[0x0D + (i shl 4)] = Inst(1, 1, "DEC $r") { write(r, alu(read(r), -1, neg = 1)) } }
    cela.forEachIndexed { i, r -> op[0x0E + (i shl 4)] = Inst(2, 2, "LD $r, d8") { write(r, read(cpu.pc + 1)) } }

    op[0x0F] = Inst(1, 1, "RRCA") { write(A, rrc(read(A).toByte(), false)) }
    op[0x1F] = Inst(1, 1, "RRA") { write(A, rr(read(A).toByte(), false)) }
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
    op[idx++] = Inst(1, 1, "HALT") { cpu.halt = true }
    op[idx++] = Inst(1, 2, "LD (HL), A") { write(read16(HL), read(A)) }
    for (from in regs) {
        op[idx++] = Inst(1, 1, "LD A, $from") { write(A, read(from)) }
    }
    op[idx++] = Inst(1, 1, "LD A, (HL)") { write(A, read(read16(HL))) }
    op[idx++] = Inst(1, 1, "LD A, A") { write(A, read(A)) }
    assert(idx == 0x80)

    // -----------------------------------
    // 0x80 - 0xBF
    // -----------------------------------
    val maths = arrayOf<Pair<String, GameBoy.(Int) -> Unit>>(
        "ADD" to { write(A, alu(read(A), it, neg = 0)) },
        "ADC" to { write(A, alu(read(A), it + Carry.get(read(F)), neg = 0)) }, // TODO: correct? idk about carry
        "SUB" to { write(A, alu(read(A), -it, neg = 1)) },
        "SBC" to { write(A, alu(read(A), -(it + Carry.get(read(F))), neg = 1)) }, // TODO: correct? idk about carry
        "AND" to {
            write(A, read(A) and it)
            write(F, (Zero.from(read(A)) xor Zero.mask) + HalfCarry.from(1))
        },
        "XOR" to {
            write(A, read(A) xor it)
            zFlagOnly(read(A))
        },
        "OR" to {
            write(A, read(A) or it)
            zFlagOnly(read(A))
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

    // -----------------------------------
    // 0xC0 - 0xFF
    // -----------------------------------
    op[0xC0] = BrInst(1, 2, 5, "RET NZ") { if (!cpu.flag(Zero)) ret() else false }
    op[0xD0] = BrInst(1, 2, 5, "RET NC") { if (!cpu.flag(Carry)) ret() else false }
    op[0xE0] = Inst(2, 3, "LD (a8), A") { write(0xFF00 + read(cpu.pc + 1), read(A)) }
    op[0xF0] = Inst(2, 3, "LD A, (a8)") { write(A, read(0xFF00 + read(cpu.pc + 1))) }

    DReg.values().forEachIndexed { i, r -> op[0xC1 + (i shl 4)] = Inst(1, 3, "POP $r") { write16(r, popS()) } }

    op[0xC2] = BrInst(3, 3, 4, "JP NZ, a16") { if (!cpu.flag(Zero)) jp() else false }
    op[0xD2] = BrInst(3, 3, 4, "JP NC, a16") { if (!cpu.flag(Carry)) jp() else false }
    op[0xE2] = Inst(1, 2, "LD (C), A") { write(0xFF00 + read(C), read(A)) }
    op[0xF2] = Inst(1, 2, "LD A, (C)") { write(A, read(0xFF00 + read(C))) }

    op[0xC3] = Inst(3, 4, "JP a16", incPC = false) { cpu.pc = read16(cpu.pc + 1).toShort() }
    op[0xF3] = Inst(1, 1, "DI") { cpu.ime = false }

    op[0xC4] = BrInst(3, 3, 6, "CALL NZ, a16") { if (!cpu.flag(Zero)) call() else false }
    op[0xD4] = BrInst(3, 3, 6, "CALL NC, a16") { if (!cpu.flag(Carry)) call() else false }

    DReg.values().forEachIndexed { i, r -> op[0xC5 + (i shl 4)] = Inst(1, 4, "PUSH $r") { pushS(read16(r)) } }

    idx = 0xC6
    for (kind in maths) {
        val name = kind.first
        val exec = kind.second
        op[idx] = Inst(2, 2, "$name A, d8") { exec(this, read(cpu.pc + 1)) }
        idx += 8
    }

    for (rstIdx in 0 until 8) {
        op[0xC7 + (rstIdx * 8)] = Inst(1, 4, "RST $rstIdx") {
            pushS(cpu.pc.int())
            cpu.pc = (rstIdx * 8).toShort()
        }
    }

    op[0xC8] = BrInst(1, 2, 5, "RET Z") { if (cpu.flag(Zero)) ret() else false }
    op[0xD8] = BrInst(1, 2, 5, "RET C") { if (cpu.flag(Carry)) ret() else false }
    op[0xE8] = Inst(2, 4, "ADD SP, s8") { cpu.sp = (cpu.sp + read(cpu.pc + 1)).toShort() }  // TODO: signed? carry?
    op[0xF8] = Inst(2, 4, "LD HL, SP+s8") { write16(HL, cpu.sp + read(cpu.pc + 1)) }    // TODO: signed? carry?

    op[0xC9] = Inst(1, 4, "RET", incPC = false) { ret() }
    op[0xD9] = Inst(1, 4, "RETI") {
        cpu.ime = true
        ret()
    }
    op[0xE9] = Inst(1, 1, "JP HL") { cpu.pc = read16(HL).toShort() }
    op[0xF9] = Inst(1, 1, "LD SP, HL") { cpu.sp = read16(HL).toShort() }

    op[0xCA] = BrInst(3, 3, 4, "JP Z, a16") { if (cpu.flag(Zero)) jp() else false }
    op[0xDA] = BrInst(3, 3, 4, "JP C, a16") { if (cpu.flag(Carry)) jp() else false }
    op[0xEA] = Inst(3, 4, "LD (a16), A") { write(read16(cpu.pc + 1), read(A)) }
    op[0xFA] = Inst(3, 4, "LD A, (a16)") { write(A, read(read16(cpu.pc + 1))) }

    op[0xFB] = Inst(1, 1, "EI") { cpu.ime = true }

    op[0xCC] = BrInst(3, 3, 6, "CALL Z, a16") { if (cpu.flag(Zero)) call() else false }
    op[0xDC] = BrInst(3, 3, 6, "CALL C, a16") { if (cpu.flag(Carry)) call() else false }

    op[0xCD] = Inst(3, 6, "CALL a16", incPC = false) { call() }
}

private fun fillExt() = InstSet.apply {
    val regInst = arrayOf<Pair<String, GameBoy.(Byte) -> Byte>>(
        "RLC" to { rlc(it, true) },
        "RRC" to { rrc(it, true) },
        "RL" to { rl(it, true) },
        "RR" to { rr(it, true) },
        "SLA" to GameBoy::sla,
        "SRA" to GameBoy::sra,
        "SWAP" to GameBoy::swap,
        "SRL" to GameBoy::srl
    )
    val bitInst = arrayOf<Pair<String, GameBoy.(Byte, Int) -> Byte>>(
        "BIT" to GameBoy::bit,
        "RES" to { b, i -> (b.int() xor (1 shl i)).toByte() },
        "SET" to { b, i -> (b + (1 shl i)).toByte() }
    )

    val regs = arrayOf(B, C, D, E, H, L)
    var idx = 0x0
    for (inst in regInst) {
        val name = inst.first
        val exec = inst.second
        for (reg in regs) ep[idx++] = Inst(2, 2, "$name $reg") { write(reg, exec(read(reg).toByte())) }
        ep[idx++] = Inst(2, 2, "$name (HL)") { write(read(read16(HL)), exec(read(read16(HL)).toByte())) }
        ep[idx++] = Inst(2, 2, "$name A") { write(A, exec(read(A).toByte())) }
    }

    for (inst in bitInst) {
        val name = inst.first
        val exec = inst.second
        for (bit in 0 until 8) {
            for (reg in regs) ep[idx++] = Inst(2, 2, "$name $bit, $reg") { write(reg, exec(read(reg).toByte(), bit)) }
            ep[idx++] = Inst(2, 2, "$name $bit, (HL)") { write(read(read16(HL)), exec(read(read16(HL)).toByte(), bit)) }
            ep[idx++] = Inst(2, 2, "$name $bit, A") { write(A, exec(read(A).toByte(), bit)) }
        }
    }
}
