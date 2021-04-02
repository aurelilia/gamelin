/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 4/3/21, 1:20 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.windows

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.VisLabel

class AboutWindow : Window("About") {

    init {
        add(HorizontalGroup().apply {
            addActor(VisLabel("gamelin v1.3.1 made by "))
            addActor(LinkLabel("ellie", "https://angm.xyz"))
        }).row()
        add(LinkLabel("Check out my other projects!", "https://github.com/anellie")).row()
        add(HorizontalGroup().apply {
            addActor(VisLabel("Made possible by "))
            addActor(LinkLabel("many amazing people.", "https://github.com/anellie/gamelin#thanks-to"))
        }).row()
        setSize(350f, 200f)
    }
}