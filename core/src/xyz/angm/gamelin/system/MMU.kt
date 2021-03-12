/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 6:54 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

class MMU {

    val rom = Array<Byte>(16_384 * 2) { 0 } // 0000-7FFF TODO bank switching
    val vram = Array<Byte>(8_192) { 0 } // 8000-9FFF
    val extRam = Array<Byte>(8_192) { 0 } // A000-BFFF
    val ram = Array<Byte>(8_192) { 0 } // C000-DFFF
    val spriteRam = Array<Byte>(160) { 0 } // FE00-FE9F
    val mmIO = Array<Byte>(128) { 0 } // FF00-FF7F TODO maybe not here...
    val zram = Array<Byte>(128) { 0 } // FF80-FFFF

    internal fun read(addr: Short): Byte {
        return when (val a = addr.toInt()) {
            in 0x0000..0x7FFF -> rom[a]
            in 0x8000..0x9FFF -> vram[a and 0x1FFF]
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF]
            in 0xC000..0xDFFF -> ram[a and 0x1FFF]
            in 0xE000..0xFDFF -> ram[a and 0x1FFF]
            in 0xFE00..0xFE9F -> spriteRam[a and 0xFF]
            in 0xFEA0..0xFEFF -> 0
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F]
            in 0xFF80..0xFFFF -> zram[a and 0x7F]
            else -> throw IndexOutOfBoundsException("what")
        }
    }

    internal fun write(addr: Short, value: Byte) {
        when (val a = addr.toInt()) {
            in 0x0000..0x7FFF -> rom[a] = value
            in 0x8000..0x9FFF -> vram[a and 0x1FFF] = value
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF] = value
            in 0xC000..0xDFFF -> ram[a and 0x1FFF] = value
            in 0xE000..0xFDFF -> ram[a and 0x1FFF] = value
            in 0xFE00..0xFE9F -> spriteRam[a and 0xFF] = value
            in 0xFEA0..0xFEFF -> Unit
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F] = value
            in 0xFF80..0xFFFF -> zram[a and 0x7F] = value
            else -> throw IndexOutOfBoundsException("what")
        }
    }
}