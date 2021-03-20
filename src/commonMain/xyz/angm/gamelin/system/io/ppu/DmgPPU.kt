/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 4:53 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io.ppu

import xyz.angm.gamelin.interfaces.TileRenderer
import xyz.angm.gamelin.system.io.MMU

internal class DmgPPU(mmu: MMU, renderer: TileRenderer = TileRenderer(mmu, 20, 18, 4f)) : PPU(mmu, renderer)