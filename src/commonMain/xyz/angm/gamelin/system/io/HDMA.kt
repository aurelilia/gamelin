/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/24/21, 11:46 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.int
import xyz.angm.gamelin.isBit

/** CGB-only HDMA transfer. TODO: Timings seem really inaccurate. */
class HDMA(private val mmu: MMU) : IODevice() {

    private var source = 0
    private var dest = 0

    private var transferring = false
    private var transferLeft = 0

    var gpuInHBlank = false

    fun step(cycles: Int) {
        if (!canTransfer()) return

        mmu.gb.advanceClock(1)
        for (i in 0 until 0x10) {
            mmu.write(dest++, mmu.read(source++))
        }
        mmu.gb.advanceClock(8)

        transferLeft -= 1
        if (transferLeft <= 0) {
            transferring = false
            transferLeft = 0x7F
        }
    }

    private fun canTransfer(): Boolean {
        val possible = transferring && (gpuInHBlank || !mmu.ppu.displayEnable)
        gpuInHBlank = false
        return possible
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.HDMA_START -> transferLeft or (transferring.int() shl 7)
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.HDMA_SRC_HIGH -> source = (source and 0xF0) or (value shl 8)
            MMU.HDMA_SRC_LOW -> source = (source and 0xFF00) or (value and 0xF0)
            MMU.HDMA_DEST_HIGH -> dest = (dest and 0xFF) or ((value and 0x1F) shl 8)
            MMU.HDMA_DEST_LOW -> dest = (dest and 0xFF00) or (value and 0xF0)
            MMU.HDMA_START -> {
                if (transferring && !value.isBit(7)) { // abort HBlank transfer
                    transferring = false
                } else {
                    source = source and 0xFFF0
                    dest = (dest and 0x1FFF) or 0x8000
                    transferLeft = value and 0x7F
                    transferring = value.isBit(7)

                    if (!transferring) {
                        mmu.gb.advanceClock(1)
                        for (i in 0 until transferLeft) {
                            for (i in 0 until 0x10) {
                                mmu.write(dest++, mmu.read(source++))
                            }
                            mmu.gb.advanceClock(8)
                        }
                    }
                }
            }
            else -> MMU.INVALID_READ
        }
    }

    fun reset() {
        source = 0
        dest = 0
        transferring = false
        transferLeft = 0
    }
}