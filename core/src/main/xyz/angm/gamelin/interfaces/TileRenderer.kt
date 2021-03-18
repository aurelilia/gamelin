/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 1:42 PM.
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
import com.badlogic.gdx.utils.Disposable
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.system.GameBoy

private const val TILE_SIZE = 8
private val colors = arrayOf(Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK)

class TileRenderer(private val gb: GameBoy, width: Int, height: Int, scale: Float) : Actor(), Disposable {

    private val pixmapA = Pixmap(width * 8, height * 8, Pixmap.Format.RGBA8888)
    private val pixmapB = Pixmap(width * 8, height * 8, Pixmap.Format.RGBA8888)
    private var current = pixmapA
    private var texture = Texture(pixmapA)

    init {
        setSize(TILE_SIZE * width * scale, TILE_SIZE * height * scale)
    }

    fun drawTile(posX: Int, posY: Int, tilePtr: Int, colorMap: (Int) -> Int) {
        for (line in 0 until TILE_SIZE) {
            val high = gb.read(tilePtr + (line * 2)).toByte()
            val low = gb.read(tilePtr + (line * 2) + 1).toByte()

            for (pixel in 0 until TILE_SIZE) {
                val colorIdx = (high.isBit(7 - pixel) shl 1) + low.isBit(7 - pixel)
                drawPixel((posX * 8) + pixel, (posY * 8) + line, colorMap(colorIdx))
            }
        }
    }

    fun drawPixel(x: Int, y: Int, colorIdx: Int) {
        val color = colors[colorIdx]
        current.setColor(color)
        current.drawPixel(x, y)
    }

    fun finishFrame() {
        val map = current
        current = if (current === pixmapA) pixmapB else pixmapA
        Gdx.app.postRunnable {
            val tex = Texture(map)
            texture.dispose()
            texture = tex
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(texture, x, y, width, height)
    }

    override fun dispose() {
        pixmapA.dispose()
        pixmapB.dispose()
        texture.dispose()
    }
}