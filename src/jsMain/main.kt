/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/28/21, 4:29 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ImageData
import org.w3c.files.FileReader
import xyz.angm.gamelin.configuration
import xyz.angm.gamelin.interfaces.device
import xyz.angm.gamelin.saveConfiguration
import xyz.angm.gamelin.stringToBytes
import xyz.angm.gamelin.system.GameBoy

lateinit var screen: HTMLCanvasElement
lateinit var screenCtx: CanvasRenderingContext2D
lateinit var screenData: ImageData

suspend fun main() {
    screen = document.getElementById("screen") as HTMLCanvasElement
    screenCtx = screen.getContext("2d") as CanvasRenderingContext2D
    screenData = screenCtx.createImageData(160.0, 144.0)
    hookOptions()

    val gb = GameBoy()
    hookFileSelect(gb)
    window.onunload = {
        gb.mmu.cart.save()
        saveConfiguration()
    }

    while (!gb.gameLoaded) delay(100)
    while (true) {
        gb.advanceDelta(0.033f)
        delay(2)
        device.sleepUntilEmpty()
    }
}

private fun hookOptions() {
    val preferCGB = document.getElementById("prefer-cgb") as HTMLInputElement
    preferCGB.checked = configuration.preferCGB
    preferCGB.addEventListener("change", {
        configuration.preferCGB = preferCGB.checked
    })

    val colorCorrect = document.getElementById("cgb-color-correct") as HTMLInputElement
    colorCorrect.checked = configuration.cgbColorCorrection
    colorCorrect.addEventListener("change", {
        configuration.cgbColorCorrection = colorCorrect.checked
    })
}

private fun hookFileSelect(gb: GameBoy) {
    val fileElem = document.getElementById("game") as HTMLInputElement
    fileElem.addEventListener("change", {
        val file = fileElem.files!!.item(0)!!
        document.getElementById("filetext")!!.innerHTML = file.name
        val reader = FileReader()
        reader.addEventListener("loadend", {
            val res = reader.result as String
            gb.loadGame(stringToBytes(res))
        })
        reader.readAsBinaryString(file)
    })
}
