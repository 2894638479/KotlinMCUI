package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure


interface Bound {
    val low: Measure
    val high: Measure
}


inline val Bound.range get() = low..high
fun Bound.contains(value: Measure) = value in range

