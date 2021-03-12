/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 10:13 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import xyz.angm.gamelin.bit
import xyz.angm.gamelin.system.GPUMode.*

private const val SCALE = 4f
private val colors = arrayOf(Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK)

class GPU(private val gb: GameBoy) : Actor() {

    private val framebuffer = Pixmap(32 * 8, 32 * 8, Pixmap.Format.RGBA8888)
    private var lastTex = Texture(framebuffer)

    private var mode = OAMScan
    private var modeclock = 0
    private var line = 0

    private val scrollX get() = gb.read(0xFF43)
    private val scrollY get() = gb.read(0xFF42)
    private val bgPalette get() = gb.read(0xFF47)

    init {
        width = 32 * 8 * SCALE
        height = 32 * 8 * SCALE
        framebuffer.drawPixel(0, 0, 0xFFFFFFF)
    }

    fun step(tCycles: Int) {
        modeclock += tCycles
        when {
            mode == OAMScan && modeclock >= 80 -> {
                modeclock = 0
                mode = Render
            }

            mode == Render && modeclock >= 172 -> {
                modeclock = 0
                mode = HBlank
                renderLine()
            }

            mode == HBlank && modeclock >= 204 -> {
                modeclock = 0
                line++
                mode = if (line == 143) VBlank else OAMScan
            }

            mode == VBlank && modeclock >= 456 -> {
                modeclock = 0
                line++
                if (line > 153) {
                    mode = OAMScan
                    line = 0
                }
            }
        }
    }

    private fun renderLine() {
        for (tile in 0x8000 until 0x9000 step 16) {
            val tileIdx = (tile - 0x8000) / 16
            renderTile(tileIdx % 16, tileIdx / 16, tile)
        }
    }

    private fun renderTile(posX: Int, posY: Int, tilePtr: Int) {
        for (line in 0 until 8) {
            val high = gb.read(tilePtr + (line * 2)).toByte()
            val low = gb.read(tilePtr + (line * 2) + 1).toByte()

            for (pixel in 0 until 8) {
                val colorIdx = (high.bit(pixel) shl 1) + low.bit(pixel)
                val color = getBGColor(colorIdx)
                framebuffer.setColor(color)
                framebuffer.drawPixel((posX * 8) + pixel, (posY * 8) + line)
            }
        }
    }

    private fun getBGColor(color: Int): Color {
        val color = (bgPalette ushr (color * 2)) and 0b11
        return colors[color]
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val tex = Texture(framebuffer)
        batch.draw(tex, x, y, width, height)
        lastTex.dispose()
        lastTex = tex
    }
}

private enum class GPUMode {
    HBlank, VBlank, OAMScan, Render
}
