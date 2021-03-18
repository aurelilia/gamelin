/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:47 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import xyz.angm.gamelin.system.io.MMU

internal actual class TileRenderer actual constructor(mmu: MMU, width: Int, height: Int, scale: Float) {
    actual fun drawTile(posX: Int, posY: Int, tilePtr: Int, colorMap: (Int) -> Int) {
    }

    actual fun drawPixel(x: Int, y: Int, colorIdx: Int) {
    }

    actual fun finishFrame() {
    }

    actual fun dispose() {
    }
}