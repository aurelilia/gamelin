/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 6:53 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.Interrupt

internal class Timer(private val gb: GameBoy) {

    private var mainClock = 0
    private var subClock = 0
    private var divClock = 0

    private var divider
        get() = gb.read(0xFF04)
        set(value) = gb.writeAny(0xFF04, value)
    private var counter
        get() = gb.read(0xFF05)
        set(value) = gb.writeAny(0xFF05, value)
    private val modulo
        get() = gb.read(0xFF06)

    private val control get() = gb.read(0xFF07)
    private val running get() = control.isBit(2)
    private val counterDivide
        get() = when (control and 3) {
            0 -> 64 // 4K
            1 -> 1 // 256K
            2 -> 4 // 64K
            else -> 16 // 16K (3)
        }

    fun step(mCycles: Int) {
        subClock += mCycles
        if (subClock >= 4) {
            mainClock++
            subClock -= 4

            divClock++
            if (divClock == 16) {
                divider++
                divClock = 0
            }
        }
        check()
    }

    private fun check() {
        if (!running) return
        if (mainClock >= counterDivide) {
            mainClock = 0
            if (counter == 255) {
                counter = modulo
                gb.requestInterrupt(Interrupt.Timer)
            } else counter++
        }
    }

    fun reset() {
        mainClock = 0
        subClock = 0
        divClock = 0
        divider = 0
        counter = 0
    }
}