/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:15 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.addrOutOfBounds
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.cpu.Interrupt

internal class Timer(private val mmu: MMU) : IODevice() {

    private var divCycleCount = 0
    private var counterTimer = 0
    private var interruptIn = 0

    private var counter = 0
    private var modulo = 0
    private var control = 0

    private var running = false
    private var counterDivider = 64

    fun step(tCycles: Int) {
        divCycleCount += tCycles
        if (interruptIn > 0) {
            interruptIn -= tCycles
            if (interruptIn <= 0) {
                counter = modulo
                mmu.requestInterrupt(Interrupt.Timer)
            }
        }

        if (running) {
            counterTimer += tCycles
            while (counterTimer >= counterDivider) {
                counterTimer -= counterDivider
                if (++counter > 0xFF) interruptIn = 4
            }
        }
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.DIV -> (divCycleCount ushr 8) and 0xFF
            MMU.TIMA -> counter
            MMU.TMA -> modulo
            MMU.TAC -> control
            else -> addrOutOfBounds(addr)
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.DIV -> divCycleCount = 0
            MMU.TIMA -> counter = value
            MMU.TMA -> modulo = value
            MMU.TAC -> {
                control = (value and 7)
                running = control.isBit(2)
                counterDivider = when (control and 3) {
                    0 -> 1024 // 4K
                    1 -> 16 // 256K
                    2 -> 64 // 64K
                    else -> 256 // 16K (3)
                }
            }
        }
    }

    fun reset() {
        divCycleCount = 0
        counterTimer = 0
        interruptIn = 0

        counter = 0
        modulo = 0
        running = false
        counterDivider = 64
    }
}