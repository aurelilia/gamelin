/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/20/21, 2:06 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.Disposable
import xyz.angm.gamelin.int
import xyz.angm.gamelin.setBit
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.cpu.Interrupt
import xyz.angm.gamelin.system.io.sound.Sound

class MMU(private val gb: GameBoy) : Disposable {

    private val vram = ByteArray(8_192) // 8000-9FFF
    private val ram = ByteArray(8_192) // C000-DFFF
    private val oam = ByteArray(160) // FE00-FE9F
    private val mmIO = ByteArray(128) // FF00-FF7F
    private val zram = ByteArray(128) // FF80-FFFF
    internal var inBios = true

    internal lateinit var cart: Cartridge
    val sound = Sound()
    internal val joypad = Joypad(this)
    internal val ppu = PPU(this)
    private val timer = Timer(this)
    private val dma = DMA(this)

    fun load(game: ByteArray) {
        cart = Cartridge.ofRom(game)
        inBios = true
    }

    fun step(cycles: Int) {
        ppu.step(cycles)
        sound.step(cycles)
        timer.step(cycles)
        dma.step(cycles)
    }

    fun reset() {
        cart.save()
        timer.reset()
        joypad.reset()
        sound.reset()
        ppu.reset()

        ram.fill(0)
        oam.fill(0)
        vram.fill(0)
        inBios = true
    }

    override fun dispose() {
        if (this::cart.isInitialized) cart.save()
        sound.dispose()
        ppu.dispose()
    }

    internal fun requestInterrupt(interrupt: Interrupt) {
        write(IF.toShort(), read(IF.toShort()).setBit(interrupt.position, 1).toByte())
    }

    fun read(addr: Short): Byte {
        // Ensure BIOS gets disabled once it's done
        if (gb.cpu.pc.int() == BIOS_PC_END) inBios = false
        return when (val a = addr.int()) {
            // Redirects
            in 0x0000..0x7FFF, in 0xA000..0xBFFF -> {
                if (inBios && addr < 0x0100) bootix[addr.toInt()]
                else cart.read(addr)
            }
            JOYP -> joypad.read()
            DMA -> dma.read(addr)
            in DIV..TAC -> timer.read(addr)
            in NR10..NR52, in WAVE_SAMPLES -> sound.read(addr)
            in LCDC..OCPD -> ppu.read(addr)

            // Direct reads
            in 0x8000..0x9FFF -> vram[a and 0x1FFF]
            in 0xC000..0xDFFF -> ram[a and 0x1FFF]
            in 0xE000..0xFDFF -> ram[a and 0x1FFF]
            in 0xFE00..0xFE9F -> oam[a and 0xFF]
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F]
            in 0xFF80..0xFFFF -> zram[a and 0x7F]

            else -> INVALID_READ.toByte()
        }
    }

    fun read(addr: Int) = read(addr.toShort()).int()

    fun read16(addr: Int): Int {
        val ls = read(addr)
        val hs = read(addr + 1)
        return ((hs shl 8) or ls)
    }

    fun write(addr: Short, value: Byte) {
        gb.debugger.writeOccurred(addr, value)
        when (val a = addr.int()) {
            // Redirects
            in 0x0000..0x7FFF, in 0xA000..0xBFFF -> cart.write(addr, value)
            JOYP -> joypad.write(value)
            DMA -> dma.write(addr, value)
            in DIV..TAC -> timer.write(addr, value)
            in NR10..NR52, in WAVE_SAMPLES -> sound.write(addr, value)
            in LCDC..OCPD -> ppu.write(addr, value)

            // Direct writes
            in 0x8000..0x9FFF -> vram[a and 0x1FFF] = value
            in 0xC000..0xDFFF -> ram[a and 0x1FFF] = value
            in 0xE000..0xFDFF -> ram[a and 0x1FFF] = value
            in 0xFE00..0xFE9F -> oam[a and 0xFF] = value
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F] = value
            in 0xFF80..0xFFFF -> zram[a and 0x7F] = value
        }
    }

    fun write(addr: Int, value: Int) = write(addr.toShort(), value.toByte())

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

        private val WAVE_SAMPLES = 0xFF30..0xFF3F

        // BOOT ROM, Bootix made by Hacktix: https://github.com/Hacktix/Bootix
        // Thank you, Hacktix!
        // This is Version 1.2, hardcoded as file support differs on targets and this is the easiest.
        private val bootix: ByteArray = byteArrayOf(
            49, -2, -1, 33, -1, -97, -81, 50, -53, 124, 32, -6, 14, 17, 33, 38, -1, 62, -128, 50, -30, 12, 62, -13, 50, -30,
            12, 62, 119, 50, -30, 17, 4, 1, 33, 16, -128, 26, -51, -72, 0, 26, -53, 55, -51, -72, 0, 19, 123, -2, 52, 32, -16,
            17, -52, 0, 6, 8, 26, 19, 34, 35, 5, 32, -7, 33, 4, -103, 1, 12, 1, -51, -79, 0, 62, 25, 119, 33, 36, -103, 14, 12,
            -51, -79, 0, 62, -111, -32, 64, 6, 16, 17, -44, 0, 120, -32, 67, 5, 123, -2, -40, 40, 4, 26, -32, 71, 19, 14, 28,
            -51, -89, 0, -81, -112, -32, 67, 5, 14, 28, -51, -89, 0, -81, -80, 32, -32, -32, 67, 62, -125, -51, -97, 0, 14,
            39, -51, -89, 0, 62, -63, -51, -97, 0, 17, -118, 1, -16, 68, -2, -112, 32, -6, 27, 122, -77, 32, -11, 24, 73, 14,
            19, -30, 12, 62, -121, -30, -55, -16, 68, -2, -112, 32, -6, 13, 32, -9, -55, 120, 34, 4, 13, 32, -6, -55, 71, 14,
            4, -81, -59, -53, 16, 23, -63, -53, 16, 23, 13, 32, -11, 34, 35, 34, 35, -55, 60, 66, -71, -91, -71, -91, 66, 60,
            0, 84, -88, -4, 66, 79, 79, 84, 73, 88, 46, 68, 77, 71, 32, 118, 49, 46, 50, 0, 62, -1, -58, 1, 11, 30, -40, 33,
            77, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 1, -32, 80
        )
    }
}