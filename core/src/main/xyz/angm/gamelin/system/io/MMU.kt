/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/16/21, 11:39 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import ktx.assets.file
import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int
import xyz.angm.gamelin.system.GameBoy

private val bootix = file("bootix_dmg.bin").readBytes()

internal class MMU(private val gb: GameBoy) {

    companion object {
        const val BIOS_PC_END = 0x0100
        const val INVALID_READ = 0xFF

        // LCD
        const val VBK = 0xFF4F
        const val LCDC = 0xFF40
        const val LY = 0xFF44
        const val LYC = 0xFF45
        const val STAT = 0xFF41
        const val SCY = 0xFF42
        const val SCX = 0xFF43
        const val WY = 0xFF4A
        const val WX = 0xFF4B
        const val BGP = 0xFF47
        const val OBP0 = 0xFF48
        const val OBP1 = 0xFF49
        const val BCPS = 0xFF68
        const val BCPD = 0xFF69
        const val OCPS = 0xFF6A
        const val OCPD = 0xFF6B

        // Interrupts
        const val IF = 0xFF0F
        const val IE = 0xFFFF

        // Timer
        const val DIV = 0xFF04
        const val TIMA = 0xFF05
        const val TMA = 0xFF06
        const val TAC = 0xFF07

        // Joypad
        const val JOYP = 0xFF00

        // DMA
        const val DMA = 0xFF46

        // Sound
        const val NR10 = 0xFF10
        const val NR11 = 0xFF11
        const val NR12 = 0xFF12
        const val NR13 = 0xFF13
        const val NR14 = 0xFF14

        const val NR21 = 0xFF16
        const val NR22 = 0xFF17
        const val NR23 = 0xFF18
        const val NR24 = 0xFF19

        const val NR30 = 0xFF1A
        const val NR31 = 0xFF1B
        const val NR32 = 0xFF1C
        const val NR33 = 0xFF1D
        const val NR34 = 0xFF1E

        const val NR41 = 0xFF20
        const val NR42 = 0xFF21
        const val NR43 = 0xFF22
        const val NR44 = 0xFF23

        const val NR50 = 0xFF24
        const val NR51 = 0xFF25
        const val NR52 = 0xFF26
    }

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
        if (gb.cpu.pc.int() == BIOS_PC_END) inBios = false
        return when (val a = addr.int()) {
            // Cannot read from:
            // FF46: DMA Transfer
            // FF18, FF1D: Sound Channels
            0xFF18, 0xFF1D -> {
                GameBoy.log.debug { "Attempted to read write-only memory at ${a.hex16()}, giving ${INVALID_READ.hex8()}. (PC: ${gb.cpu.pc.hex16()})" }
                INVALID_READ.toByte()
            }

            // Redirects
            JOYP -> gb.joypad.read()
            in DIV..TAC -> gb.timer.read(a).toByte()
            in NR10..NR52 -> gb.sound.read(addr.int()).toByte()

            else -> readAny(addr)
        }
    }

    fun write(addr: Short, value: Byte) {
        gb.debugger.writeOccured(addr, value)
        when (val a = addr.int()) {
            // Cannot write to:
            // FF44: Current PPU scan line
            0xFF44 -> GameBoy.log.debug { "Attempted to write ${value.hex8()} to read-only memory location ${a.hex16()}, ignored. (PC: ${gb.cpu.pc.hex16()})" }

            // Redirects
            JOYP -> gb.joypad.write(value)
            in DIV..TAC -> gb.timer.write(a, value.int())
            in NR10..NR52 -> gb.sound.write(addr.int(), value.int())

            // Special behavior for:
            // FF46: OAM DMA
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
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F]
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
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F] = value
            in 0xFF80..0xFFFF -> zram[a and 0x7F] = value
            else -> throw IndexOutOfBoundsException("what ${a.toString(16)}")
        }
    }
}