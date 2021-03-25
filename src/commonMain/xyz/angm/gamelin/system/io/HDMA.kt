/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 1:24 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.isBit

/** CGB-only GDMA/HDMA transfer.
 * Note that "CPU stopping" during transfer is simply
 * accomplished by advancing the clock without it. */
class HDMA(private val mmu: MMU) : IODevice() {

    private var source = 0
    private var dest = 0
    private var hBlankTransferring = false
    private var transferLeft = 0
    var gpuInHBlank = false

    fun step() {
        if (!canAdvanceHblank()) return

        mmu.gb.advanceClock(1)
        for (i in 0 until 0x10) {
            mmu.write(dest++, mmu.read(source++))
        }
        mmu.gb.advanceClock(8)

        transferLeft -= 1
        if (transferLeft < 0) {
            hBlankTransferring = false
            transferLeft = 0xFF
        }
    }

    /** Can another line of data be transferred?
     * True if an HDMA transfer is active and HBlank was just entered. */
    private fun canAdvanceHblank(): Boolean {
        val possible = hBlankTransferring && (gpuInHBlank || !mmu.ppu.displayEnable)
        gpuInHBlank = false
        return possible
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.HDMA_START -> transferLeft
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.HDMA_SRC_HIGH -> source = (source and 0xF0) or (value shl 8)
            MMU.HDMA_SRC_LOW -> source = (source and 0xFF00) or (value and 0xF0)
            MMU.HDMA_DEST_HIGH -> dest = (dest and 0xFF) or ((value and 0x1F) shl 8)
            MMU.HDMA_DEST_LOW -> dest = (dest and 0x1F00) or (value and 0xF0)
            MMU.HDMA_START -> {
                if (hBlankTransferring && !value.isBit(7)) abortHBlank()
                else {
                    dest = (dest and 0x1FFF) or 0x8000
                    transferLeft = value and 0x7F
                    hBlankTransferring = value.isBit(7)
                    if (!hBlankTransferring) gdmaTransfer()
                }
            }
            else -> MMU.INVALID_READ
        }
    }

    private fun abortHBlank() {
        hBlankTransferring = false
        transferLeft = transferLeft or 0x80
    }

    /** Immediately execute a GDMA transfer. */
    private fun gdmaTransfer() {
        mmu.gb.advanceClock(1)
        for (i in 0 until transferLeft) {
            for (i in 0 until 0x10) {
                mmu.write(dest++, mmu.read(source++))
            }
            mmu.gb.advanceClock(8)
        }
    }

    fun reset() {
        source = 0
        dest = 0
        hBlankTransferring = false
        transferLeft = 0
        gpuInHBlank = false
    }
}