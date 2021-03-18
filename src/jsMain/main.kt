/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 11:34 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

import com.soywiz.korge.Korge
import com.soywiz.korge.view.Circle
import com.soywiz.korim.color.Colors

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
    val circle = Circle(radius = 20.0)
    addChild(circle)
}