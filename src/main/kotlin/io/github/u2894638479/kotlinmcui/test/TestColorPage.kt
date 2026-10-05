package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.ADouble
import io.github.u2894638479.kotlinmcui.prop.HDouble
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.widget.Colors

/**
 * Every colour widget at once: the composite editor, the two palettes, the two hue palettes and the
 * hexadecimal field, plus the core colour slider the composite edits its channels with. It doubles
 * as the `color` page of the in-game verification run, where the triangles are only drawn if the
 * backend has the triangle-list capability.
 */
context(ctx: DslContext)
fun TestColorPage() = ScrollableColumn(Modifier.padding(5.scaled)) {
    // A colour at full saturation and value, so the marker sits exactly on the square's corner and
    // on the triangle's apex - the place where it would spill out of the widget if it could.
    val prop = local { Color.ofHSV(0.08,1.0,1.0) }
    Colors.ColorEdit(prop,id = "editor")
    Row(Modifier.padding(5.scaled)) {
        // One row, so a single screenshot shows every widget: the two palettes, the hue triangle,
        // the hue bar in both orientations and the hexadecimal field.
        Colors.ColorSquare(prop,Modifier.size(50.scaled,50.scaled),"square")
        Colors.ColorTriangle(prop,Modifier.size(50.scaled,46.scaled),"triangle")
        Colors.ColorHueTriangle(prop,Modifier.size(50.scaled,46.scaled),"hue-triangle")
        Colors.ColorHue(prop,Modifier.size(12.scaled,50.scaled),Axis.Vertical,"hue-vertical")
        Colors.ColorHue(prop,Modifier.size(50.scaled,12.scaled),Axis.Horizontal,"hue-horizontal")
        Colors.ColorText(prop,Modifier.width(80.scaled),"hex")
    }
    // The channel sliders on their own: the same core slider the composite's strip is built from,
    // which carries each channel's own colour ramp on its track.
    Row(Modifier.padding(5.scaled)) {
        Slider(Modifier.width(120.scaled),Axis.Horizontal,ADouble,prop,id = "alpha") {
            TextFlatten { ADouble.name.emit(); ": ".emit(); ADouble.toString(prop.value).emit() }
        }
        Slider(Modifier.width(120.scaled),Axis.Horizontal,HDouble,prop,id = "hue") {
            TextFlatten { HDouble.name.emit(); ": ".emit(); HDouble.toString(prop.value).emit() }
        }
    }
}.defaultBackground()
