package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.move
import io.github.u2894638479.kotlinmcui.dsl.decorator.perspective
import io.github.u2894638479.kotlinmcui.dsl.decorator.rotate
import io.github.u2894638479.kotlinmcui.dsl.decorator.scale
import io.github.u2894638479.kotlinmcui.dsl.decorator.shear
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.ColorRect
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.editBoxBackground
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip
import kotlin.math.PI

context(ctx: DslContext)
fun TestRotationPage() = ScrollableColumn {
    var rad by Config.Slider(0.0..PI,"rotation rad") {}
    var scale by Config.Slider(0.5..2.0,"scale") {}
    var x by Config.Slider(-100..100,"moveX") {}
    var y by Config.Slider(-100..100,"moveY") {}
    Column {
        val rad2 = local { 0.0 }
        Row {
            Config.Slider(rad2,0.0..1.0,"test") {}
            Simple.Button("test") {}.simpleTooltip("Rotate!","rotate")
        }
        ColorRect(Modifier.height(50.scaled),Color.RED) {}
        Row(Modifier.weight(0.0)) {
            ColorRect(Modifier,Color.BLUE) {}
            val selfRenderPreedit = local { false }
            Column {
                Simple.Button("test2"){}.simpleTooltip("Nested Rotate!","nested")
                Config.BoolButton(selfRenderPreedit,"preedit") {}
            }.rotate(rad2.value)
            ColorRect(Modifier,Color.GREEN) {}
            val text = local { "editable" }
            EditableText(Modifier,text, selfRenderPreedit = selfRenderPreedit.value) {}.editBoxBackground()
        }.move(x.scaled,y.scaled)
    }.rotate(rad).scale(scale)

    // ── Shear ──────────────────────────────────────────────────────────
    Simple.Text { "shear" }
    val shxProp = Config.Slider(-1.0..1.0, "shearX", id = "shx")
    val shyProp = Config.Slider(-1.0..1.0, "shearY", id = "shy")
    var shx by shxProp; var shy by shyProp
    Row {
        ColorRect(Modifier.height(80.scaled), Color.RED) {}
        ColorRect(Modifier.height(80.scaled), Color.GREEN) {}
        ColorRect(Modifier.height(80.scaled), Color.BLUE) {}
    }.shear(shx, shy)

    // ── Perspective ────────────────────────────────────────────────────
    Simple.Text { "perspective" }
    val pxProp = Config.Slider(-0.005..0.005, "perspectiveX", id = "px")
    val pyProp = Config.Slider(-0.005..0.005, "perspectiveY", id = "py")
    var px by pxProp; var py by pyProp
    Row {
        ColorRect(Modifier.height(80.scaled), Color.RED) {}
        ColorRect(Modifier.height(80.scaled), Color.GREEN) {}
        ColorRect(Modifier.height(80.scaled), Color.BLUE) {}
    }.perspective(px, py)
}