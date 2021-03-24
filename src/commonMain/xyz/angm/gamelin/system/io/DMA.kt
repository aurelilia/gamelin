/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/24/21, 10:45 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

/** DMA transfer, as part of both DMG and CGB.
 * This implementation simply ticks down the transfer time
 * and transfers everything at once at the end. */
class DMA(private val mmu: MMU): IODevice() {

    private var reg = 0xFF
    private var transferTimeLeft = 0
    private var transferFrom = 0
    val transferring get() = transferTimeLeft in 1..644

    fun step(cycles: Int) {
        if (transferTimeLeft <= 0) return
        transferTimeLeft -= cycles
        if (transferring) return
        for (dest in 0xFE00..0xFE9F) mmu.write(dest, mmu.read(transferFrom++))
    }

    override fun read(addr: Int) = reg

    override fun write(addr: Int, value: Int) {
        reg = value
        transferFrom = (value * 0x100) and 0xFFFF
        transferTimeLeft = 648
    }

    fun reset() {
        reg = 0xFF
        transferTimeLeft = 0
        transferFrom = 0
    }
}