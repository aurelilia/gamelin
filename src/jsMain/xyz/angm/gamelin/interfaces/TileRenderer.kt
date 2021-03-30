/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/30/21, 9:25 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import org.khronos.webgl.Uint32Array
import org.khronos.webgl.set
import screenCtx
import screenData

/** TileRenderer implementation using an HTML canvas.
 * Ignores width/height, as JS only has the screen and no debugging tools. */
internal actual class TileRenderer actual constructor(tileWidth: Int, tileHeight: Int) {

    private val pixels = Uint32Array(screenData.data.buffer)

    actual fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        val idx = (x + (y * 160))
        val color = r or (g shl 8) or (b shl 16) or (255 shl 24) // RGBA8888 but reversed for some reason
        pixels[idx] = color
    }

    actual fun finishFrame() = screenCtx.putImageData(screenData, 0.0, 0.0)

    actual fun dispose() {}
}