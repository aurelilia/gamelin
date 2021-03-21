/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 12:05 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.Disposable
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.DReg
import xyz.angm.gamelin.system.io.MMU

open class Debugger : Disposable {

    var emuHalt = true
    var pcBreak = 0
    var pcBreakEnable = false
    var writeBreak = 0
    var writeBreakEnable = false

    var loggingEnable = false
    protected val logger = StringBuilder()
    private var pc = 0
    private var nopCount = 0

    fun preAdvance(gb: GameBoy) {
        pc = gb.cpu.pc.int()
        if (loggingEnable && !gb.mmu.bootromOn) {
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
            logger.append("DIV = ${gb.read(MMU.DIV).hex16()} IE = ${gb.read(MMU.IE).hex16()} IF = ${gb.read(MMU.IF).hex16()} ")
            logger.append("TIMA = ${gb.read(MMU.TIMA).hex16()} TMA = ${gb.read(MMU.TMA).hex16()} TAC = ${gb.read(MMU.TAC).hex16()} ")
            logger.append("AF = ${gb.read16(DReg.AF).hex16()} BC = ${gb.read16(DReg.BC).hex16()} ")
            logger.appendLine("DE = ${gb.read16(DReg.DE).hex16()} HL = ${gb.read16(DReg.HL).hex16()} SP = ${gb.readSP().hex16()}")

            // 1 MB, very big logs (500MB+) can cause the emu to run out of heap space trying to write it to a file
            if (logger.length > 1_000_000) flushLog()
        }
    }

    fun postAdvance(gb: GameBoy) {
        if (pcBreakEnable && pcBreak == gb.cpu.pc.int()) emuHalt = true
    }

    override fun dispose() {
        if (logger.isNotEmpty()) {
            if (nopCount > 0) logger.appendLine("${pc.hex16()} NOP $nopCount TIMES")
            flushLog()
        }
    }

    protected open fun flushLog() {
        // Overriding debuggers can write to a file here.
        logger.clear()
    }

    open fun writeOccurred(addr: Short, value: Byte) {
        if (writeBreakEnable && writeBreak == addr.int()) emuHalt = true
    }
}