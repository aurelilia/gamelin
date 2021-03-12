/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 1:37 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.system.DReg.*
import xyz.angm.gamelin.system.Flag.*
import xyz.angm.gamelin.system.Reg.*

class Inst(val size: Short, val cycles: Int, val name: String, val execute: GameBoy.() -> Unit)

object InstSet {

    internal val op = arrayOfNulls<Inst>(Byte.MAX_VALUE.toInt())
    internal val extOp = arrayOfNulls<Inst>(Byte.MAX_VALUE.toInt())

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
    // 0x00 - 0x3F
    op[0x00] = Inst(1, 1, "NOP") { }
    op[0x10] = Inst(1, 1, "STOP") { throw InterruptedException("RIP") }
    op[0x20] = null
    op[0x30] = null

    op[0x01] = Inst(3, 3, "LD BC, d16") { write(BC, read16(cpu.pc + 1)) }
    op[0x11] = Inst(3, 3, "LD DE, d16") { write(DE, read16(cpu.pc + 1)) }
    op[0x21] = Inst(3, 3, "LD HL, d16") { write(HL, read16(cpu.pc + 1)) }
    op[0x31] = Inst(3, 3, "LD SP, d16") { writeSP(read(cpu.pc + 1)) }

    op[0x02] = Inst(1, 2, "LD (BC), A") { write(read(BC), read(A)) }
    op[0x12] = Inst(1, 2, "LD (DE), A") { write(read(DE), read(A)) }
    op[0x22] = Inst(1, 2, "LD (HL+), A") { write(readModify(HL, +1), read(A)) }
    op[0x32] = Inst(1, 2, "LD (HL-), A") { write(readModify(HL, -1), read(A)) }

    op[0x03] = Inst(1, 2, "INC BC") { write(BC, read(BC) + 1) }
    op[0x13] = Inst(1, 2, "INC DE") { write(DE, read(DE) + 1) }
    op[0x23] = Inst(1, 2, "INC HL") { write(HL, read(HL) + 1) }
    op[0x33] = Inst(1, 2, "INC SP") { writeSP(cpu.sp + 1) }

    op[0x04] = Inst(1, 1, "INC B") { write(B, alu(read(B), 1, neg = 0)) }
    op[0x14] = Inst(1, 1, "INC D") { write(D, alu(read(D), 1, neg = 0)) }
    op[0x24] = Inst(1, 1, "INC H") { write(H, alu(read(H), 1, neg = 0)) }
    op[0x34] = Inst(1, 3, "INC (HL)") { write(read(HL), alu(read(read(HL)), 1, neg = 0)) }

    op[0x05] = Inst(1, 1, "DEC B") { write(B, alu(read(B), -1, neg = 1)) }
    op[0x15] = Inst(1, 1, "DEC D") { write(D, alu(read(D), -1, neg = 1)) }
    op[0x25] = Inst(1, 1, "DEC H") { write(H, alu(read(H), -1, neg = 1)) }
    op[0x35] = Inst(1, 3, "DEC (HL)") { write(read(HL), alu(read(read(HL)), -1, neg = 1)) }

    op[0x06] = Inst(2, 2, "LD B, d8") { write(B, read(cpu.pc + 1)) }
    op[0x16] = Inst(2, 2, "LD D, d8") { write(D, read(cpu.pc + 1)) }
    op[0x26] = Inst(2, 2, "LD H, d8") { write(H, read(cpu.pc + 1)) }
    op[0x36] = Inst(1, 3, "LD (HL), d8") { write(read(HL), read(cpu.pc + 1)) }

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
}