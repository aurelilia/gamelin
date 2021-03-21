/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 6:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.MMU

const val TILE_SIZE = 8

/** Represents the canvas the system's LCD is displayed on.
 * @param mmu The MMU of the GameBoy to render.
 * @param width The width of the LCD in tiles; 20 for the LCD and 32/16 for BG/VRAM display windows
 * @param height The height of the LCD in tiles; 18 for the LCD and 32/24 for BG/VRAM display windows
 * @param scale The scale for each pixel, usually in powers of 2. Might be ignored on some platforms. */
internal expect class TileRenderer(mmu: MMU, width: Int, height: Int, scale: Float) {
    /** Draw a single pixel at the given location, with the given RGB888 colors. */
    fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int)
    /** Called when VBLANK occurs and a full frame has rendered, to allow the renderer to display it. */
    fun finishFrame()
    /** Dispose all underlying resources like pixmaps. */
    fun dispose()
}