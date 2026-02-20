package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px


interface Bound {
    val low: Measure
    val high: Measure
}


inline val Bound.range get() = low..high
fun Bound.contains(value: Measure) = value in range

fun Bound.expand(low: Measure = 0.px,high: Measure = 0.px) = object :Bound {
    override val low get() = this@expand.low - low
    override val high get() = this@expand.high + high
}
