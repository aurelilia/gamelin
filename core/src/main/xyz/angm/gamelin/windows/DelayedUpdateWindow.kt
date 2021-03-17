/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/17/21, 10:22 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

abstract class DelayedUpdateWindow(name: String, private val period: Float) : Window(name) {

    protected var sinceUpdate = 0f

    override fun act(delta: Float) {
        super.act(delta)
        sinceUpdate += delta
        if (sinceUpdate > period) {
            refresh()
            sinceUpdate = 0f
        }
    }

    protected abstract fun refresh()
}