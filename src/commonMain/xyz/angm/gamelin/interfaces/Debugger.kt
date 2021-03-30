/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 11:18 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.Disposable
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.DReg

/** A debugger that is part of the system and able to hook into it.
 * Besides breakpoints, it also offers per-cycle CPU state logging useful for
 * exact emulator behavior debugging.
 *
 * @property emuHalt If the emulator is currently halted and should not continue. `true` at boot and after hitting a breakpoint. */
open class Debugger : Disposable {

    var emuHalt = true
    var enableBreakpoints = false
    val breakpoints = ArrayList<Breakpoint>()

    var loggingEnable = false
    protected val logger = StringBuilder()
    private var nopCount = 0

    /** Called right before executing the next instruction. */
    fun preAdvance(gb: GameBoy) {
        if (loggingEnable && !gb.mmu.bootromOn) {
            val pc = gb.cpu.pc
            val inst = gb.getNextInst()

            if (inst.name == "NOP") {
                nopCount++
                return
            }
            if (nopCount > 0) {
                logger.appendLine("${pc.hex16()} NOP $nopCount TIMES")
                nopCount = 0
            }

            logger.append("${pc.hex16()} ${inst.name.padEnd(20)} ${gb.read16(pc + 1).hex16()}  ")
            logger.append("AF = ${gb.read16(DReg.AF).hex16()} BC = ${gb.read16(DReg.BC).hex16()} ")
            logger.appendLine("DE = ${gb.read16(DReg.DE).hex16()} HL = ${gb.read16(DReg.HL).hex16()} SP = ${gb.readSP().hex16()}")

            // 1 MB, very big logs (500MB+) can cause the emu to run out of heap space trying to write it to a file
            if (logger.length > 1_000_000) flushLog()
        }
    }

    /** Called after executing an instruction. */
    fun postAdvance(gb: GameBoy) {
        if (enableBreakpoints && breakpoints.any { it.onPC && it.location == gb.cpu.pc }) emuHalt = true
    }

    override fun dispose() {
        if (logger.isNotEmpty()) flushLog()
    }

    protected open fun flushLog() {
        // Overriding debuggers can write to a file here.
        logger.clear()
    }

    /** Called by MMU when the system has written a value. */
    open fun writeOccurred(addr: Short, value: Byte) {
        if (enableBreakpoints && breakpoints.any { it.onWrite && it.location == addr }) emuHalt = true
    }

    /** @property location Location of the breakpoint
     * @property onPC BP triggers on PC if true
     * @property onWrite BP triggers on write if true */
    data class Breakpoint(val location: Short, var onPC: Boolean, var onWrite: Boolean)
}