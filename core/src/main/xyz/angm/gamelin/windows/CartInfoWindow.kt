/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 1:23 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visLabel
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.io.Cartridge

class CartInfoWindow(private val gb: GameBoy) : DelayedUpdateWindow("Cartridge Info", 1f) {

    private val tab = KVisTable(true)

    init {
        add(tab).pad(10f)
        tab.defaults().left()
        refresh()
        pack()
    }

    override fun refresh() {
        tab.clearChildren()
        tab.run {
            if (gb.gameLoaded) {
                visLabel("Reported Title: ${gb.mmu.cart.getTitle()}") { it.row() }
                visLabel("Reported Title (extended): ${gb.mmu.cart.getTitle(true)}") { it.row() }
                visLabel("Controller Type: ${gb.mmu.cart::class.simpleName} (${Cartridge.KIND.hex16()}: ${gb.read(Cartridge.KIND).hex8()})") { it.row() }
                visLabel("ROM Banks: ${gb.mmu.cart.romBankCount}") { it.row() }
                visLabel("RAM Banks: ${gb.mmu.cart.ramBankCount}") { it.row() }
                visLabel("Destination: ${if (gb.read(Cartridge.DESTINATION) == 0) "Japan" else "Global"}") { it.row() }
                visLabel(
                    "CGB Flag: ${gb.read(Cartridge.CGB_FLAG).hex16()} (${
                        when (gb.read(Cartridge.CGB_FLAG)) {
                            0x80 -> "Supports CGB Mode"
                            0xC0 -> "CGB only"
                            else -> "DMG only"
                        }
                    })"
                ) { it.row() }
            } else {
                visLabel("No game loaded")
            }
        }
        pack()
    }
}