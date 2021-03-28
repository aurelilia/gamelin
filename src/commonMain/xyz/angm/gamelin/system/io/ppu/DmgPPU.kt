/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 11:00 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.bit
import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.io.MMU

/** PPU of the original DMG GameBoy. */
internal class DmgPPU(mmu: MMU, renderer: TileRenderer = TileRenderer(20, 18)) : PPU(mmu, renderer) {

    override fun renderLine() {
        if (!displayEnable) return
        if (bgEnable) {
            renderBG()
            if (windowEnable) renderWindow()
        } else {
            clearLine()
        }
        if (objEnable) {
            renderObjs()
            usedXObjCoords.fill(-1)
        }
    }

    override fun renderBGOrWindow(scrollX: Int, startX: Int, endX: Int, mapAddr: Int, mapLine: Int, correctTileAddr: Boolean) {
        var tileX = scrollX and 7
        val tileY = mapLine and 7
        var tileAddr = mapAddr + ((mapLine / 8) * 0x20) + (scrollX ushr 3)
        var tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2)
        var high = mmu.vram[tileDataAddr + 1]
        var low = mmu.vram[tileDataAddr]

        for (tileIdxAddr in startX until endX) {
            val colorIdx = (high.bit(7 - tileX) shl 1) + low.bit(7 - tileX)
            if (colorIdx != 0) setPixelOccupied(tileIdxAddr, line)
            renderer.drawPixel(tileIdxAddr, line, getBGColor(colorIdx))

            if (++tileX == 8) {
                tileX = 0
                tileAddr = if (correctTileAddr && (tileAddr and 0x1F) == 0x1F) tileAddr - 0x20 else tileAddr
                tileAddr++
                tileDataAddr = bgTileDataAddr(mmu.vram[tileAddr]) + (tileY * 2)
                high = mmu.vram[tileDataAddr + 1]
                low = mmu.vram[tileDataAddr]
            }
        }
    }

    override fun getBGAddrAdjust(tileAddr: Int) = 0

    // A list of sprite's X coords; used to ensure overlapping sprites get correct ordering
    private val usedXObjCoords = IntArray(10)

    override fun allowObj(objCount: Int): Boolean {
        for (idx in 0 until objCount) {
            if (usedXObjCoords[idx] == Sprite.x) return false // X coord already occupied by another sprite
        }
        usedXObjCoords[objCount] = Sprite.x
        return true
    }

    override fun drawObjPixel(x: Int, y: Int, colorIdx: Int, dmgPalette: Int) {
        renderer.drawPixel(x, y, getColor(dmgPalette, colorIdx))
    }

    override fun vramObjAddrOffset() = 0

    override fun setPixelOccupied(x: Int, y: Int) {
        bgOccupiedPixels[(x * 144) + y] = true
    }
}