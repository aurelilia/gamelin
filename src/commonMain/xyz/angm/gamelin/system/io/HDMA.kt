/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/22/21, 7:48 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.int
import xyz.angm.gamelin.isBit

/** CGB-only HDMA transfer. TODO: Actual transfer still unimplemented. */
class HDMA(private val mmu: MMU) : IODevice() {

    private var source = 0
    private var dest = 0
    private var transferring = false
    private var transferLeft = 0
    private var isHBlank = false

    fun step(cycles: Int) {
        if (!transferring) return
        for (i in 0 until transferLeft) mmu.write(dest++, mmu.read(source++))
        transferring = false
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.HDMA_SRC_HIGH -> source ushr 8
            MMU.HDMA_SRC_LOW -> source and 0xFF
            MMU.HDMA_DEST_HIGH -> dest ushr 8
            MMU.HDMA_DEST_LOW -> dest and 0xFF
            MMU.HDMA_START -> transferLeft or (transferring.int() shl 7)
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.HDMA_SRC_HIGH -> source = (source and 0xFF) or (value ushr 8)
            MMU.HDMA_SRC_LOW -> source = (source and 0xFF00) or (value and 0xF0)
            MMU.HDMA_DEST_HIGH -> dest = (dest and 0xFF) or (value ushr 8)
            MMU.HDMA_DEST_LOW -> dest = (dest and 0xFF00) or (value and 0xF0)
            MMU.HDMA_START -> {
                if (transferring && isHBlank && !value.isBit(7)) { // abort HBlank transfer
                    transferring = false
                } else {
                    transferring = true
                    transferLeft = value and 0x8F
                    isHBlank = value.isBit(7)
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
        isHBlank = false
    }
}