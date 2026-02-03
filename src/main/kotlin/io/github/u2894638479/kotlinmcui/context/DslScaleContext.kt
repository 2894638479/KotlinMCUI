package io.github.u2894638479.kotlinmcui.context

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px


interface DslScaleContext {
    val scale: Double
}

context(ctx: DslScaleContext)
val Double.scaled get() = px * ctx.scale

context(ctx: DslScaleContext)
val Float.scaled get() = px * ctx.scale

context(ctx: DslScaleContext)
val Int.scaled get() = px * ctx.scale

context(ctx: DslScaleContext)
val Measure.unscaled get() = pixelsOrElse { 0.0 } / ctx.scale