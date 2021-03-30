/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/29/21, 1:20 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import HqnxEffect
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.collections.*
import xyz.angm.gamelin.*
import xyz.angm.gamelin.system.io.ppu.PPU

/** TileRenderer using 2 pixmaps for buffering the image, as well as a texture
 * that contains the last finished pixmap.
 * Each time a frame is finished, the current pixmap is swapped.
 * This causes a 2-3 frame lag, but prevents the user from seeing mid-frame display states.
 *
 * Additionally, since the GB is running in a separate thread to the GUI,
 * the renderer's [finishFrame] is also used to execute any runnables that the GUI
 * thread queued (@see [runInGbThread]).
 * This is to prevent race conditions that can happen when the GUI thread directly tries
 * to access the gb. */
internal actual class TileRenderer actual constructor(private val tileWidth: Int, private val tileHeight: Int) : Actor() {

    private val pixmapA = Pixmap(tileWidth * 8, tileHeight * 8, Pixmap.Format.RGB888)
    private val pixmapB = Pixmap(tileWidth * 8, tileHeight * 8, Pixmap.Format.RGB888)
    private var current = pixmapA
    private var texture: Texture? = null
    private var hqx = getHqx()

    private val queuedRunnables = GdxArray<() -> Unit>()

    constructor(width: Int, height: Int, scale: Float) : this(width, height) {
        setGBScale(scale)
    }

    inline fun drawTile(posX: Int, posY: Int, tilePtr: Int, colorMap: (Int) -> Int) {
        for (line in 0 until TILE_SIZE) {
            val baseAddr = (tilePtr + (line * 2))
            val high = gb.mmu.vram[baseAddr]
            val low = gb.mmu.vram[baseAddr + 1]

            for (pixel in 0 until TILE_SIZE) {
                val colorIdx = (high.bit(7 - pixel) shl 1) + low.bit(7 - pixel)
                val c = PPU.dmgColors[colorMap(colorIdx)]
                drawPixel((posX * 8) + pixel, (posY * 8) + line, c, c, c)
            }
        }
    }

    actual fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        var idx = (x + (y * 160)) * 3
        current.pixels.put(idx++, r.toByte())
        current.pixels.put(idx++, g.toByte())
        current.pixels.put(idx, b.toByte())
    }

    actual fun finishFrame() {
        val map = current
        current = if (current === pixmapA) pixmapB else {
            if (config.enableRewind && !rewinding) SaveState.rewindPoint()
            pixmapA
        }
        Gdx.app.postRunnable {
            val tex = Texture(map)
            texture?.dispose()
            texture = tex
        }
        queuedRunnables.forEach { it() }
        queuedRunnables.clear()

        if (rewinding) gb.debugger.emuHalt = true
    }

    /** Queue the given runnable to be executed when the next frame has finished. */
    fun queueRunnable(run: () -> Unit) = queuedRunnables.add(run)

    /** Set the scale of this actor to the given value. 1x is regular pixel-perfect tiles. */
    fun setGBScale(scale: Float) {
        setSize(TILE_SIZE * tileWidth * scale, TILE_SIZE * tileHeight * scale)
    }

    /** Compares the current screen contents to the given PNG file.
     * Used by acid2 tests to ensure correct PPU output. */
    fun compareTo(file: FileHandle): Boolean {
        val cmp = Pixmap(file)
        for (x in 0 until current.width) {
            for (y in 0 until current.height) {
                if (cmp.getPixel(x, y) != current.getPixel(x, y)) {
                    cmp.dispose()
                    return false
                }
            }
        }
        cmp.dispose()
        return true
    }

    fun beforeRender() {
        hqx?.renderToBuffer(texture ?: return)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(hqx?.dstBuffer?.colorBufferTexture ?: texture ?: return, x, y, width, height)
    }

    fun hqxLevelChanged() {
        hqx = getHqx()
    }

    private fun getHqx() = if (config.hqxLevel in 2..4) HqnxEffect(config.hqxLevel) else null

    actual fun dispose() {
        pixmapA.dispose()
        pixmapB.dispose()
        texture?.dispose()
        hqx?.dispose()
    }
}