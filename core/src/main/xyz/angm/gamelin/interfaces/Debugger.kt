/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 7:01 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.DReg

open class Debugger : Disposable {

    var emuHalt = true
    var pcBreak = 0
    var pcBreakEnable = false
    var writeBreak = 0
    var writeBreakEnable = false

    var loggingEnable = false
    private val logger = StringBuilder()
    private var pc = 0
    private var nopCount = 0

    fun preAdvance(gb: GameBoy) {
        pc = gb.cpu.pc.int()
        if (loggingEnable && !gb.mmu.inBios) {
            val inst = gb.getNextInst()
            when {
                inst.name == "NOP" -> nopCount++
                nopCount > 0 -> {
                    logger.appendLine("${pc.hex16()} NOP $nopCount TIMES")
                    nopCount = 0
                }
                else -> {
                    logger.append("${pc.hex16()} ${inst.name.padEnd(20)} ${gb.read16(pc + 1).hex16()}  ")
                    logger.append("AF = ${gb.read16(DReg.AF).hex16()} BC = ${gb.read16(DReg.BC).hex16()} ")
                    logger.appendLine("DE = ${gb.read16(DReg.DE).hex16()} HL = ${gb.read16(DReg.HL).hex16()} SP = ${gb.readSP().hex16()}")
                }
            }
        }
    }

    fun postAdvance(gb: GameBoy) {
        if (pcBreakEnable && pcBreak == gb.cpu.pc.int()) emuHalt = true
    }

    override fun dispose() {
        if (logger.isNotEmpty()) {
            if (nopCount > 0) logger.appendLine("${pc.hex16()} NOP $nopCount TIMES")
            Gdx.files.local("gamelin.log").writeString(logger.toString(), false)
        }
    }

    open fun writeOccured(addr: Short, value: Byte) {
        if (writeBreakEnable && writeBreak == addr.int()) emuHalt = true
    }
}