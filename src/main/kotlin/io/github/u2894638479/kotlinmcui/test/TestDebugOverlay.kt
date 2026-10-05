package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.container.DebugOverlay
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.align
import io.github.u2894638479.kotlinmcui.modifier.width

context(ctx: DslContext)
fun DebugOverlay.content(alpha: Double = 0.5) = ScrollableColumn {
    val left = Modifier.align { left() }.width(Measure.AUTO_MIN)
    infix fun String.display(value: Any?) = TextFlatten(left) {
        "${this@display}: $value".emit()
    }.background(Color(128,128,128,128))
    infix fun String.display2f(value: Double) = display(String.format("%.2f",value))
    "screen name" display screenTitle
    "gui scale" display guiScale
    "local props" display localProps
    "stable props" display stableProps
    "static props" display staticProps
    "fps" display2f frameRate
    "smooth fps" display2f frameRateSmooth
    "frame time" display2f frameTimeMs
    "smooth frame time" display2f frameTimeMsSmooth
    "frame render time" display2f frameRenderTimeMs
    "dsl render time" display2f dslRenderTimeMs
    "backend render time" display2f backendRenderTimeMs
    "components" display components
    "focused" display (focusedId != null)
    "hovered" display (hoveredId != null)
    "focused depth" display focusedDepth
    "hovered depth" display hoveredDepth
    "mouse" display "(${mouse.x.pixelsOrElse { 0.0 }}, ${mouse.y.pixelsOrElse { 0.0 }})"
}