/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 9:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import xyz.angm.gamelin.system.GPUMode.*
import java.awt.Color

private const val SCALE = 4f
private val colors = arrayOf(Color.WHITE, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK)

class GPU(private val gb: GameBoy) : Actor() {

    private val framebuffer = Pixmap(160, 144, Pixmap.Format.RGBA8888)
    private var lastTex = Texture(framebuffer)

    private var mode = OAMScan
    private var modeclock = 0
    private var line = 0

    init {
        width = 160 * SCALE
        height = 144 * SCALE
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
        TODO()
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
