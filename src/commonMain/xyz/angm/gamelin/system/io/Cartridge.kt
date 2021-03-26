/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/26/21, 8:33 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.bit
import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.DateTime
import xyz.angm.gamelin.interfaces.FileSystem
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.setBit
import kotlin.jvm.Transient
import kotlin.math.max
import kotlin.math.min

/** A game that the system is playing.
 * @property rom The raw ROM file of this game. */
abstract class Cartridge(@Transient var rom: ByteArray) : IODevice() {

    internal val romBankCount = (2 shl rom[ROM_BANKS].int())
    internal val ramBankCount = when (rom[RAM_BANKS].int()) {
        0 -> 0
        2 -> 1
        3 -> 4
        4 -> 16
        5 -> 8
        else -> throw UnsupportedOperationException("Unknown cartridge controller")
    }
    internal val supportsCGB = read(CGB_FLAG).isBit(7)
    internal val requiresCGB = read(CGB_FLAG) == CGB_ONLY

    private val ram = loadRAM()
    protected var ramEnable = false
    private val ramAvailable get() = ram.isNotEmpty() && ramEnable

    protected var romBank = 1
    protected var ramBank = 0

    override fun read(addr: Int): Int {
        return when (addr) {
            in 0x0000..0x3FFF -> rom[addr]
            in 0x4000..0x7FFF -> rom[(addr and 0x3FFF) + (0x4000 * romBank)]
            in 0xA000..0xBFFF -> if (ramAvailable) ram[(addr and 0x1FFF) + (0x2000 * ramBank)] else MMU.INVALID_READ.toByte()
            else -> throw UnsupportedOperationException("Not cartridge")
        }.int()
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0xA000..0xBFFF -> if (ramAvailable) ram[(addr and 0x1FFF) + (0x2000 * ramBank)] = value
                .toByte()
        }
    }

    fun getTitle(extended: Boolean = false): String {
        val str = StringBuilder()
        for (byte in 0x134..if (extended) 0x0142 else 0x013E) { // Title in Game ROM
            val value = read(byte)
            if (value == 0x00) break
            str.append(value.toChar())
        }
        return str.toString()
    }

    /** Save the game RAM to disk, if applicable. */
    open fun save() {
        if (ramBankCount > 0) FileSystem.saveRAM(getFileSystemName(), ram)
    }

    /** Tries to load the RAM from disk. */
    private fun loadRAM(): ByteArray {
        var ram: ByteArray? = null
        if (ramBankCount > 0) ram = FileSystem.loadRAM(getFileSystemName())
        return ram ?: ByteArray(ramBankCount * 0x2000)
    }

    protected fun getFileSystemName() = "${getTitle(extended = true)}${rom[DESTINATION]}"

    companion object {

        const val CGB_FLAG = 0x0143
        const val CGB_ONLY = 0xC0
        const val DMG_AND_CGB = 0x80

        const val KIND = 0x0147
        const val ROM_BANKS = 0x0148
        const val RAM_BANKS = 0x0149
        const val DESTINATION = 0x014A
        const val BANK_COUNT_1MB = 64

        fun ofRom(rom: ByteArray): Cartridge {
            return when (rom[KIND].int()) {
                in 0x01..0x03 -> MBC1(rom)
                in 0x05..0x06 -> MBC2(rom)
                in 0x0F..0x10 -> MBC3Timer(rom)
                in 0x11..0x13 -> MBC3(rom)
                in 0x19..0x1E -> MBC5(rom)
                else -> NoMBC(rom)
            }
        }
    }
}

/** Cartridges with no MBC: 1 ROM bank, no RAM */
class NoMBC(rom: ByteArray) : Cartridge(rom)

/** Cartridges with the MBC1 controller. */
class MBC1(rom: ByteArray) : Cartridge(rom) {

    private var ramMode = false
    private var rom0Bank = 0
    private var bank2 = 0

    override fun read(addr: Int): Int {
        return when (addr) {
            in 0x0000..0x3FFF -> rom[addr + 0x4000 * rom0Bank].int()
            else -> super.read(addr)
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnable = (value and 0x0F) == 0x0A
            in 0x2000..0x3FFF -> {
                romBank = max((value and 0x1F), 1)
                updateBank2()
            }
            in 0x4000..0x5FFF -> {
                bank2 = value and 0x03
                updateBank2()
            }
            in 0x6000..0x7FFF -> {
                ramMode = value.isBit(0)
                updateBank2()
            }
            else -> super.write(addr, value)
        }
    }

    private fun updateBank2() {
        ramBank = if (ramBankCount == 4 && ramMode) bank2 else 0
        romBank = (romBank and 0x1F)
        if (romBankCount >= BANK_COUNT_1MB) romBank += bank2 shl 5
        romBank %= romBankCount
        rom0Bank = if (ramMode && romBankCount >= BANK_COUNT_1MB) (bank2 shl 5) % romBankCount else 0
    }
}

/** Cartridges with the MBC2 controller. */
class MBC2(rom: ByteArray) : Cartridge(rom) {

    private val mbc2ram = ByteArray(512)

    override fun read(addr: Int): Int {
        return when (addr) {
            in 0xA000..0xBFFF -> if (ramEnable) mbc2ram[(addr and 0x1FF)].int() else MMU.INVALID_READ
            else -> super.read(addr)
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x3FFF -> {
                if (addr.isBit(8)) romBank = max(value and 0x0F, 1) % romBankCount
                else ramEnable = (value and 0x0F) == 0x0A
            }
            in 0xA000..0xBFFF -> {
                if (ramEnable) mbc2ram[(addr and 0x1FF)] = (value or 0xF0).toByte()
            }
        }
    }
}

/** Cartridges with the MBC3 controller. */
open class MBC3(rom: ByteArray) : Cartridge(rom) {

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnable = (value and 0x0F) == 0x0A
            in 0x2000..0x3FFF -> romBank = max(value, 1) % romBankCount
            in 0x4000..0x5FFF -> ramBank = (value and 0x03) % ramBankCount
            else -> super.write(addr, value)
        }
    }
}

/** Cartridges with the MBC3 controller *including a timer*. */
class MBC3Timer(rom: ByteArray) : MBC3(rom) {

    private val rtc = FileSystem.loadRTC(getFileSystemName()) ?: DateTime()
    private var rtcRegister = -1
    private var rtcLatchPrepare = false

    override fun read(addr: Int): Int {
        return when {
            addr in 0xA000..0xBFFF && rtcRegister in 0..3 -> rtc.get(rtcRegister)
            else -> super.read(addr)
        }
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x4000..0x5FFF -> {
                if (value in 0x08..0x0C) rtcRegister = min(value - 0x08, 4)
                else {
                    ramBank = (value and 0x03) % ramBankCount
                    rtcRegister = -1
                }
            }

            in 0x6000..0x7FFF -> {
                if (value == 0x01 && rtcLatchPrepare) {
                    rtcLatchPrepare = false
                    rtc.latch()
                }
                rtcLatchPrepare = rtcLatchPrepare || value == 0x00
            }

            in 0xA000..0xBFFF -> {
                if (rtcRegister >= 0) rtc.set(rtcRegister, value)
                else super.write(addr, value)
            }

            else -> super.write(addr, value)
        }
    }

    override fun save() {
        super.save()
        FileSystem.saveRTC(getFileSystemName(), rtc)
    }
}

/** Cartridges with the MBC5 controller. */
class MBC5(rom: ByteArray) : Cartridge(rom) {

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnable = (value and 0x0F) == 0x0A
            in 0x2000..0x2FFF -> romBank = (romBank and 0x100) or (value % romBankCount)
            in 0x3000..0x3FFF -> romBank = romBank.setBit(8, value.bit(0)) % romBankCount
            in 0x4000..0x5FFF -> ramBank = (value and 0x03) % ramBankCount
            else -> super.write(addr, value)
        }
    }
}