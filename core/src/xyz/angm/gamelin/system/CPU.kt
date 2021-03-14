/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/14/21, 5:20 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.int
import xyz.angm.gamelin.isBit
import kotlin.experimental.and

internal class CPU(private val gb: GameBoy) {

    var pc: Short = 0
    var sp: Short = 0
    var ime = false
    var halt = false
    val regs = ByteArray(Reg.values().size)

    fun nextInstruction() {
        val ime = this.ime
        val inst = gb.getNextInst()
        var cyclesTaken = when (inst) {
            is BrInst -> {
                if (inst.executeBr(gb)) inst.cyclesWithBranch
                else {
                    pc = (pc + inst.size).toShort()
                    inst.cycles
                }
            }
            else -> {
                inst.execute(gb)
                if (inst.incPC) pc = (pc + inst.size).toShort()
                inst.cycles
            }
        }

        gb.gpu.step(tCycles = cyclesTaken * 4)
        val interCycles = checkInterrupts(ime)
        gb.gpu.step(tCycles = interCycles * 4)
        gb.clock += cyclesTaken + interCycles
    }

    private fun checkInterrupts(ime: Boolean): Int {
        if (!ime) return 0
        for (interrupt in Interrupt.values()) {
            if (interrupt.isSet(gb.read(Interrupt.IE)) && interrupt.isSet(gb.read(Interrupt.IF))) {
                halt = false
                gb.write(Interrupt.IF, gb.read(Interrupt.IF) xor (1 shl interrupt.position))
                this.ime = false
                gb.pushS(pc.int())
                pc = interrupt.handlerAddr
                return 3
            }
        }
        return 0
    }

    internal fun write(reg: Reg, value: Byte) {
        // Register F only allows writing the 4 high/flag bits
        val regVal = if (reg == Reg.F) (value.int() and 0xF0).toByte() else value
        regs[reg.idx] = regVal
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

internal enum class DReg(val high: Reg, val low: Reg) {
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
    fun isSet(reg: Int) = reg.isBit(position)
    fun from(value: Int) = (if (value != 0) 1 else 0) shl position
}

internal enum class Interrupt(val position: Int, val handlerAddr: Short) {
    VBlank(0, 0x0040),
    HBlank(1, 0x0048),
    Timer(2, 0x0050),
    Serial(3, 0x0058),
    Keyboard(4, 0x0060);

    fun isSet(reg: Int) = reg.isBit(position)

    companion object {
        const val IF = 0xFF0F
        const val IE = 0xFFFF
    }
}
