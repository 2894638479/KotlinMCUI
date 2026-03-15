package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.rotate
import io.github.u2894638479.kotlinmcui.functions.ui.ColorRect
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip
import kotlin.math.PI

context(ctx: DslContext)
fun TestRotationPage() = ScrollableColumn {
    var rad by Config.slider(0.0..PI,"rotation rad") {}
    Column {
        Row {
            Config.slider(0.0..1.0,"test") {}
            Simple.Button("test") {}.simpleTooltip("Rotate!","rotate")
        }
        ColorRect(Modifier.height(50.scaled),Color.RED) {}
    }.rotate(rad)
}