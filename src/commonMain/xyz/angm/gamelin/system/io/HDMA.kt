/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 5:51 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.isBit

class HDMA : IODevice() {

    private var source = 0
    private var dest = 0
    private var transferring = false
    private var transferLeft = 0
    private var isHBlank = false

    fun step(cycles: Int) {
        if (!transferring) return
    }

    override fun read(addr: Int): Int {
        return when (addr) {
            MMU.HDMA_SRC_HIGH -> source ushr 8
            MMU.HDMA_SRC_LOW -> source and 0xFF
            MMU.HDMA_DEST_HIGH -> dest ushr 8
            MMU.HDMA_DEST_LOW -> dest and 0xFF
            MMU.HDMA_START -> transferLeft
            else -> MMU.INVALID_READ
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            MMU.HDMA_SRC_HIGH -> source = (source and 0xFF) or (value ushr 8)
            MMU.HDMA_SRC_LOW -> source = (source and 0xFF00) or value
            MMU.HDMA_DEST_HIGH -> dest = (dest and 0xFF) or (value ushr 8)
            MMU.HDMA_DEST_LOW -> dest = (dest and 0xFF00) or value
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