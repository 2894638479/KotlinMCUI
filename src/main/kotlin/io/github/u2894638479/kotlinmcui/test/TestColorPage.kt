package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.widget.ColorChannel
import io.github.u2894638479.kotlinmcui.widget.ColorEdit
import io.github.u2894638479.kotlinmcui.widget.ColorHue
import io.github.u2894638479.kotlinmcui.widget.ColorHueTriangle
import io.github.u2894638479.kotlinmcui.widget.ColorSlider
import io.github.u2894638479.kotlinmcui.widget.ColorSquare
import io.github.u2894638479.kotlinmcui.widget.ColorText
import io.github.u2894638479.kotlinmcui.widget.ColorTriangle
import io.github.u2894638479.kotlinmcui.widget.Controls

/**
 * Every colour widget at once: the composite editor, the two palettes, and the sliders and the
 * text field on their own. It doubles as the `color` page of the in-game verification run, where
 * the triangle is only drawn if the backend has the triangle-list capability.
 */
context(ctx: DslContext)
fun TestColorPage() = ScrollableColumn(Modifier.padding(5.scaled)) {
    // A colour at full saturation and value, so the marker sits exactly on the square's corner and
    // on the triangle's apex - the place where it would spill out of the widget if it could.
    val prop = local { Color.ofHSV(0.08,1.0,1.0) }
    Controls.ColorEdit(prop,id = "editor")
    Row(Modifier.padding(5.scaled)) {
        // One row, so a single screenshot shows every widget: the two palettes, the hue triangle,
        // the hue bar in both orientations and the hexadecimal field.
        Controls.ColorSquare(prop,Modifier.size(50.scaled,50.scaled),"square")
        Controls.ColorTriangle(prop,Modifier.size(50.scaled,46.scaled),"triangle")
        Controls.ColorHueTriangle(prop,Modifier.size(50.scaled,46.scaled),"hue-triangle")
        Controls.ColorHue(prop,Modifier.size(12.scaled,50.scaled),Axis.Vertical,"hue-vertical")
        Controls.ColorHue(prop,Modifier.size(50.scaled,12.scaled),Axis.Horizontal,"hue-horizontal")
        Controls.ColorText(prop,Modifier.width(80.scaled),"hex")
    }
}.defaultBackground()
