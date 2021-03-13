/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 2:50 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.GameBoy

const val TILE_SIZE = 8
private const val SCALE = 4f
private val colors = arrayOf(Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK)

class TileRenderer(private val gb: GameBoy, width: Int, height: Int) : Actor() {

    private val pixmap = Pixmap(width * 8, height * 8, Pixmap.Format.RGBA8888)
    private var lastTex = Texture(pixmap)

    init {
        setSize(TILE_SIZE * width * (SCALE / 2), TILE_SIZE * height * (SCALE / 2))
    }

    fun drawTile(posX: Int, posY: Int, tilePtr: Int, colorMap: (Int) -> Int) {
        for (line in 0 until TILE_SIZE) {
            val high = gb.read(tilePtr + (line * 2)).toByte()
            val low = gb.read(tilePtr + (line * 2) + 1).toByte()

            for (pixel in 0 until TILE_SIZE) {
                val colorIdx = (high.isBit(7 - pixel) shl 1) + low.isBit(7 - pixel)
                val color = colors[colorMap(colorIdx)]
                pixmap.setColor(color)
                pixmap.drawPixel((posX * 8) + pixel, (posY * 8) + line)
            }
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val tex = Texture(pixmap)
        batch.draw(tex, x, y - height, width * 2, height * 2)
        lastTex.dispose()
        lastTex = tex
    }
}