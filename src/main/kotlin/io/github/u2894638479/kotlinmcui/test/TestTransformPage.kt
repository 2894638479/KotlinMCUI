package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.move
import io.github.u2894638479.kotlinmcui.dsl.decorator.perspective
import io.github.u2894638479.kotlinmcui.dsl.decorator.rotate
import io.github.u2894638479.kotlinmcui.dsl.decorator.scale
import io.github.u2894638479.kotlinmcui.dsl.ui.ColorRect
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.defaultBackground
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.widget.Presets

/**
 * Exercises the transform pipeline, including the third row of the pose matrix that only
 * the perspective projection uses.
 *
 * The bands are given explicit sizes: a full-width band would sit far from the projection
 * centre, where perspective magnifies it by an order of magnitude.
 */
context(ctx: DslContext)
fun TestTransformPage() {
    Column(Modifier.padding(10.scaled)) {
        Presets.Text { "rotate" }
        Row(Modifier.size(240.scaled, 30.scaled)) {
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.RED) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.GREEN) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.BLUE) {}
        }.rotate(0.35)

        Presets.Text { "perspective" }
        Row(Modifier.size(240.scaled, 30.scaled)) {
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.RED) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.GREEN) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.BLUE) {}
        }.perspective(0.002, 0.0008)

        Presets.Text { "scale + move" }
        Row(Modifier.size(240.scaled, 30.scaled)) {
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.RED) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.GREEN) {}
            ColorRect(Modifier.size(80.scaled, 30.scaled), Color.BLUE) {}
        }.scale(1.4).move(30.scaled, 0.scaled)
    }.defaultBackground()
}
