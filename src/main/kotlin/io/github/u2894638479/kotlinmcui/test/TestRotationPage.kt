package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.move
import io.github.u2894638479.kotlinmcui.functions.decorator.rotate
import io.github.u2894638479.kotlinmcui.functions.decorator.scale
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.ColorRect
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
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
        val rad2 by 0.0.remember.property
        Row {
            Config.Slider(rad2,0.0..1.0,"test") {}
            Simple.Button("test") {}.simpleTooltip("Rotate!","rotate")
        }
        ColorRect(Modifier.height(50.scaled),Color.RED) {}
        Row(Modifier.weight(0.0)) {
            ColorRect(Modifier,Color.BLUE) {}
            Column {
                Simple.Button("test2"){}.simpleTooltip("Nested Rotate!","nested")
            }.rotate(rad2.value)
            ColorRect(Modifier,Color.GREEN) {}
        }.move(x.scaled,y.scaled)
    }.rotate(rad).scale(scale)
}