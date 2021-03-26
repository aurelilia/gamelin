/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 8:38 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

const val TILE_SIZE = 8

/** Represents the canvas the system's LCD is displayed on.
 * @param tileWidth The width of the LCD in tiles; 20 for the LCD and 32/16 for BG/VRAM display windows
 * @param tileHeight The height of the LCD in tiles; 18 for the LCD and 32/24 for BG/VRAM display windows */
internal expect class TileRenderer(tileWidth: Int, tileHeight: Int) {
    /** Draw a single pixel at the given location, with the given RGB888 colors. */
    fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int)
    /** Called when VBLANK occurs and a full frame has rendered, to allow the renderer to display it. */
    fun finishFrame()
    /** Dispose all underlying resources like pixmaps. */
    fun dispose()
}