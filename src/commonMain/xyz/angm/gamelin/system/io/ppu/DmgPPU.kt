/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 1:40 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.io.MMU

internal class DmgPPU(mmu: MMU, renderer: TileRenderer = TileRenderer(mmu, 20, 18, 4f)) : PPU(mmu, renderer) {

    override fun renderLine() {
        if (!displayEnable) return
        if (bgEnable) {
            renderBG()
            if (windowEnable) renderWindow()
        } else {
            clearLine()
        }
        if (objEnable) renderObjs()
    }

    override fun drawBGorWindowPixel(x: Int, y: Int, colorIdx: Int, tileAddr: Int) {
        renderer.drawPixel(x, y, getBGColor(colorIdx))
    }

    override fun drawObjPixel(x: Int, y: Int, colorIdx: Int, dmgPalette: Int) {
        renderer.drawPixel(x, y, getColor(dmgPalette, colorIdx))
    }

    override fun vramSpriteAddrOffset() = 0

    override fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = true
    }
}