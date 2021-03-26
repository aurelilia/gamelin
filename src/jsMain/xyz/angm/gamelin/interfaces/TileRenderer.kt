/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 8:58 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import screenCtx
import screenData

/** TileRenderer implementation using an HTML canvas.
 * Ignores width/height. */
internal actual class TileRenderer actual constructor(tileWidth: Int, tileHeight: Int) {

    actual fun drawPixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        var idx = (x + (y * 160)) * 4
        val dat = screenData.data.unsafeCast<Uint16Array>()
        dat[idx++] = r.toShort()
        dat[idx++] = g.toShort()
        dat[idx++] = b.toShort()
        dat[idx] = 255
    }

    actual fun finishFrame() {
        screenCtx.putImageData(screenData, 0.0, 0.0)
    }

    actual fun dispose() {
    }
}