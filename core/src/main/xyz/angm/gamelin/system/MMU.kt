/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 3:19 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import ktx.assets.file
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int
import kotlin.experimental.and

private const val INVALID_READ = 0xFF.toByte()
private val bootix = file("bootix_dmg.bin").readBytes()

internal class MMU(private val gb: GameBoy) {

    // Is there any real reason to have so many byte arrays instead
    // of just one big one?
    private lateinit var rom: ByteArray // ROM: 0000-7FFF TODO bank switching
    private val vram = ByteArray(8_192) // 8000-9FFF
    private val extRam = ByteArray(8_192) // A000-BFFF
    private val ram = ByteArray(8_192) // C000-DFFF
    private val oam = ByteArray(160) // FE00-FE9F
    private val mmIO = ByteArray(128) // FF00-FF7F
    private val zram = ByteArray(128) // FF80-FFFF
    internal var inBios = true

    fun load(game: ByteArray) {
        rom = game
        inBios = true
    }

    fun read(addr: Short): Byte {
        // Ensure BIOS gets disabled once it's done
        if (gb.cpu.pc.int() == 0x100) inBios = false
        return when (val a = addr.int()) {
            // Cannot read from:
            // FF46: DMA Transfer
            // FF18, FF1D: Sound Channels
            0xFF18, 0xFF1D -> {
                GameBoy.log.debug { "Attempted to read write-only memory at ${a.hex16()}, giving ${INVALID_READ.hex8()}. (PC: ${gb.cpu.pc.hex16()})" }
                INVALID_READ
            }

            else -> readAny(addr)
        }
    }

    fun write(addr: Short, value: Byte) {
        gb.debugger.writeOccured(addr, value)
        when (val a = addr.int()) {
            // Cannot write to:
            // FF44: Current PPU scan line
            0xFF44 -> GameBoy.log.debug { "Attempted to write ${value.hex8()} to read-only memory location ${a.hex16()}, ignored. (PC: ${gb.cpu.pc.hex16()})" }

            // Special behavior for:
            // FF04: Timer Divider: Reset it
            // FF07: Timer Control: Only lower 3 bits are used
            // FF46: OAM DMA
            0xFF04 -> writeAny(addr, 0)
            0xFF07 -> writeAny(addr, value and 7)
            0xFF46 -> { //TODO timing & blocking reads
                var source = value.int() shl 8
                for (dest in 0xFE00..0xFE9F) write(dest.toShort(), read(source++.toShort()))
            }

            else -> writeAny(addr, value)
        }
    }

    fun readAny(addr: Short): Byte {
        return when (val a = addr.int()) {
            in 0x0000..0x7FFF -> {
                if (inBios && addr < 0x0100) bootix[a]
                else rom[a]
            }
            in 0x8000..0x9FFF -> vram[a and 0x1FFF]
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF]
            in 0xC000..0xDFFF -> ram[a and 0x1FFF]
            in 0xE000..0xFDFF -> ram[a and 0x1FFF]
            in 0xFE00..0xFE9F -> oam[a and 0xFF]
            in 0xFEA0..0xFEFF -> 0
            0xFF00 -> gb.keyboard.read()
            in 0xFF01..0xFF7F -> mmIO[a and 0x7F]
            in 0xFF80..0xFFFF -> zram[a and 0x7F]
            else -> throw IndexOutOfBoundsException("what ${a.toString(16)}")
        }
    }

    fun writeAny(addr: Short, value: Byte) {
        when (val a = addr.int()) {
            in 0x0000..0x7FFF -> rom[a] = value
            in 0x8000..0x9FFF -> vram[a and 0x1FFF] = value
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF] = value
            in 0xC000..0xDFFF -> ram[a and 0x1FFF] = value
            in 0xE000..0xFDFF -> ram[a and 0x1FFF] = value
            in 0xFE00..0xFE9F -> oam[a and 0xFF] = value
            in 0xFEA0..0xFEFF -> Unit
            0xFF00 -> gb.keyboard.write(value)
            in 0xFF01..0xFF7F -> mmIO[a and 0x7F] = value
            in 0xFF80..0xFFFF -> zram[a and 0x7F] = value
            else -> throw IndexOutOfBoundsException("what ${a.toString(16)}")
        }
    }
}