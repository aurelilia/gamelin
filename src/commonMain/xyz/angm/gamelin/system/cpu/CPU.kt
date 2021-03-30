/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:05 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.cpu

import xyz.angm.gamelin.bit
import xyz.angm.gamelin.int
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.io.MMU

/** Container for the SM83 CPU found in the GameBoy. */
internal class CPU(private val gb: GameBoy) {

    var pc: Short = 0
    var sp: Short = 0
    var ime = false
    var regIE = 0
    var regIF = 0
    var halt = false
    var haltBug = false
    var prepareSpeedSwitch = false
    val regs = ByteArray(Reg.values().size)

    /** Execute the next instruction, stepping the entire system forward
     * as required. Will also check interrupts after executing an instruction and possibly
     * advance the clock to account for interrupt handling; making the exact amount
     * that this steps the internal clock by nondeterministic. */
    fun nextInstruction() {
        val ime = this.ime

        if (halt) {
            gb.advanceClock(1)
            halt = (gb.read(MMU.IF) and gb.read(MMU.IE) and 0x1F) == 0
        } else {
            val inst = gb.getNextInst()
            if (inst.preCycles != 0) gb.advanceClock(inst.preCycles)
            val cyclesTaken = when (inst)  {
                is BrInst -> {
                    if (inst.executeBr(gb)) inst.cyclesWithBranch
                    else {
                        pc = (pc + inst.size).toShort()
                        inst.cycles
                    }
                }
                else -> {
                    if (haltBug) pc--
                    haltBug = false

                    inst.execute(gb)
                    if (inst.incPC) pc = (pc + inst.size).toShort()
                    inst.cycles
                }
            }

            gb.advanceClock(cyclesTaken)
        }

        if (checkInterrupts(ime && this.ime)) gb.advanceClock(5)
    }

    /** Checks for interrupts and returns true if one has been fired,
     * to allow the CPU to step the system 5 more M-Cycles forward (SM83 takes 5 cycles to handle interrupts). */
    private fun checkInterrupts(ime: Boolean): Boolean {
        val bits = regIE and regIF
        if (!ime || (bits == 0)) return false
        for (bit in 0 until 5) {
            if (bits.isBit(bit)) {
                halt = false
                regIF = regIF.setBit(bit, 0)
                this.ime = false
                gb.pushS(pc.int())
                pc = Interrupt.addresses[bit]
                return true
            }
        }
        return false
    }

    internal fun switchSpeed() {
        gb.tSpeedMultiplier = if (gb.tSpeedMultiplier == 1) 2 else 1
        prepareSpeedSwitch = false
        for (i in 0 until 1025) gb.advanceClock(2)
    }

    fun write(reg: Reg, value: Byte) {
        // Register F only allows writing the 4 high/flag bits
        val regVal = if (reg == Reg.F) (value.int() and 0xF0).toByte() else value
        regs[reg.ordinal] = regVal
    }

    fun flag(flag: Flag) = regs[Reg.F.ordinal].isBit(flag.position)
    fun flagVal(flag: Flag) = regs[Reg.F.ordinal].bit(flag.position)
    fun flag(flag: Flag, value: Int) = flag(flag, value != 0)
    fun flag(flag: Flag, value: Boolean) {
        regs[Reg.F.ordinal] = regs[Reg.F.ordinal].setBit(flag.position, if (value) 1 else 0).toByte()
    }

    fun reset() {
        pc = 0
        sp = 0
        ime = false
        regIE = 0
        regIF = 0
        halt = false
        haltBug = false
        prepareSpeedSwitch = false
        regs.fill(0)
    }
}

/** All the CPU registers of the SM83 */
internal enum class Reg {
    A, B, C, D, E, F, H, L
}

/** All the "double" registers. Order is chosen for [Inst],
 * to allow easy iteration for creating the instruction set. */
internal enum class DReg(val high: Reg, val low: Reg) {
    BC(Reg.B, Reg.C),
    DE(Reg.D, Reg.E),
    HL(Reg.H, Reg.L),
    AF(Reg.A, Reg.F)
}

/** All math flags in the F register. */
internal enum class Flag(val position: Int) {
    Zero(7),
    Negative(6),
    HalfCarry(5),
    Carry(4);

    val mask get() = 1 shl position
    fun isSet(reg: Int) = reg.isBit(position)
    fun from(value: Int) = if (value != 0) 1 shl position else 0
}

/** All interrupts the SM83 can process. */
internal enum class Interrupt(val handlerAddr: Short) {
    VBlank(0x0040),
    STAT(0x0048),
    Timer(0x0050),
    Serial(0x0058),
    Joypad(0x0060);

    companion object {
        val addresses = values().map { it.handlerAddr }.toShortArray()
    }
}
