/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 7:34 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import xyz.angm.gamelin.bit
import xyz.angm.gamelin.gb
import xyz.angm.gamelin.system.io.MMU
import xyz.angm.gamelin.system.io.ppu.PPU

/** TileRenderer using 2 pixmaps for buffering the image, as well as a texture
 * that contains the last finished pixmap.
 * Each time a frame is finished, the current pixmap is swapped.
 * This causes a 2-3 frame lag, but prevents the user from seeing mid-frame display states. */
internal actual class TileRenderer actual constructor(mmu: MMU, width: Int, height: Int, scale: Float) : Actor() {

    private val pixmapA = Pixmap(width * 8, height * 8, Pixmap.Format.RGBA8888)
    private val pixmapB = Pixmap(width * 8, height * 8, Pixmap.Format.RGBA8888)
    private var current = pixmapA
    private var texture: Texture? = null

    init {
        setSize(TILE_SIZE * width * scale, TILE_SIZE * height * scale)
    }

    inline fun drawTile(posX: Int, posY: Int, tilePtr: Int, colorMap: (Int) -> Int) {
        for (line in 0 until TILE_SIZE) {
            val high = gb.mmu.read(tilePtr + (line * 2)).toByte()
            val low = gb.mmu.read(tilePtr + (line * 2) + 1).toByte()

            for (pixel in 0 until TILE_SIZE) {
                val colorIdx = (high.bit(7 - pixel) shl 1) + low.bit(7 - pixel)
                val c = PPU.dmgColors[colorMap(colorIdx)]
                drawPixel((posX * 8) + pixel, (posY * 8) + line, c, c, c)
            }
        }
    }

    actual fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        val color = (r shl 24) or (g shl 16) or (b shl 8) or 255 // RGBA8888
        current.setColor(color)
        current.drawPixel(x, y)
    }

    actual fun finishFrame() {
        val map = current
        current = if (current === pixmapA) pixmapB else pixmapA
        Gdx.app.postRunnable {
            val tex = Texture(map)
            texture?.dispose()
            texture = tex
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(texture ?: return, x, y, width, height)
    }

    actual fun dispose() {
        pixmapA.dispose()
        pixmapB.dispose()
        texture?.dispose()
    }

    companion object {
        private val colors = arrayOf(Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK)
    }
}