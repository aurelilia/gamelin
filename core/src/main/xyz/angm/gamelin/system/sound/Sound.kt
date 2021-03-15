/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 9:24 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.sound

import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.AudioOutput
import xyz.angm.gamelin.isBit
import xyz.angm.gamelin.isBit_
import xyz.angm.gamelin.system.GameBoy

private val MASKS = intArrayOf(
    0x80, 0x3f, 0x00, 0xff, 0xbf,
    0xff, 0x3f, 0x00, 0xff, 0xbf,
    0x7f, 0xff, 0x9f, 0xff, 0xbf,
    0xff, 0xff, 0x00, 0x00, 0xbf,
    0x00, 0x00, 0x70,
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
)

// TODO: Proper FF26 reg
class Sound(private val gb: GameBoy) {

    private val sink = AudioOutput()

    private val channelControl get() = gb.read(0xFF24)
    private val soundToggle get() = gb.read(0xFF26).isBit(7)
    private val leftVolume get() = (channelControl ushr 4) and 7
    private val rightVolume get() = channelControl and 7
    private val selection get() = gb.read(0xFF25)

    private val channels = arrayOf(Square1(gb), Square2(gb), Wave(gb), Noise(gb))
    private var enabled = false

    fun step(tCycles: Int) {
        if (!enabled) return
        for (i in 0 until tCycles) cycle()
    }

    private fun cycle() {
        val selection = selection
        var left = 0
        var right = 0

        channels.forEachIndexed { i, mode ->
            val out = mode.cycle()
            if (selection.isBit(i + 4)) left += out
            if (selection.isBit(i)) right += out
        }
        left /= 4
        right /= 4
        left *= leftVolume
        right *= rightVolume
        sink.play(left, right)
    }

    fun channelRead(addr: Short) = readUnmasked(addr) or MASKS[addr - 0xFF10]
    fun channelWrite(addr: Short, value: Byte) = channelFromAddr(addr).write(addr, value.int())
    private fun readUnmasked(addr: Short) = channelFromAddr(addr).read(addr)
    private fun channelFromAddr(addr: Short) = channels[((addr - 0xFF10) and 0xFF) / 5]

    fun enableWritten(value: Byte) {
        val enableBit = value.isBit_(7)
        if (enabled && !enableBit) stop()
        else if (!enabled && enableBit) start()
        enabled = enableBit
        gb.writeAny(0xFF26, if (enabled) 0x8F else 0)
    }

    private fun start() {
        for (i in 0xff10..0xff23) {
            var v = 0
            // lengths should be preserved
            if (i == 0xff11 || i == 0xff16 || i == 0xff20) { // channel 1, 2, 4 lengths
                v = readUnmasked(i.toShort()) and 63
            } else if (i == 0xff1b) { // channel 3 length
                v = readUnmasked(i.toShort())
            }
            channelWrite(i.toShort(), v.toByte())
        }
        channels.forEach(Channel::start)
    }

    private fun stop() {
        channels.forEach(Channel::stop)
    }
}
