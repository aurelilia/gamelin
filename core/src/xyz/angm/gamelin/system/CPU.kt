/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 10:23 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.int
import kotlin.experimental.and

internal class CPU(private val gb: GameBoy) {

    var pc: Short = 0x100
    var sp: Short = 0
    var ime = false
    var halt = false
    val regs = ByteArray(Reg.values().size)

    fun nextInstruction(): Inst {
        val inst = gb.getNextInst()

        when (inst) {
            is BrInst -> if (!inst.executeBr(gb)) pc = (pc + inst.size).toShort()
            else -> {
                inst.execute(gb)
                if (inst.incPC) pc = (pc + inst.size).toShort()
            }
        }

        gb.gpu.step(tCycles = inst.cycles * 4)
        return inst
    }

    fun flag(flag: Flag) = flagVal(flag) == 1
    fun flagVal(flag: Flag) = ((regs[Reg.F.idx].int() ushr flag.position) and 1)

    fun flag(flag: Flag, value: Int) {
        regs[Reg.F.idx] = ((regs[Reg.F.idx] and flag.invMask.toByte()) + flag.from(value)).toByte()
    }
}

internal enum class Reg(val idx: Int) {
    A(0),
    B(1),
    C(2),
    D(3),
    E(4),
    F(5),
    H(6),
    L(7),
}

internal enum class DReg(val low: Reg, val high: Reg) {
    BC(Reg.B, Reg.C),
    DE(Reg.D, Reg.E),
    HL(Reg.H, Reg.L),
    AF(Reg.A, Reg.F)
}

internal enum class Flag(val position: Int) {
    Zero(7),
    Negative(6),
    HalfCarry(5),
    Carry(4);

    val mask get() = 1 shl position
    val invMask get() = (1 shl position) xor 0xFF

    fun get(reg: Int) = (reg and mask) shr position
    fun isSet(reg: Int) = ((reg and mask) shr position) != 0
    fun from(value: Int) = (if (value != 0) 1 else 0) shl position
}