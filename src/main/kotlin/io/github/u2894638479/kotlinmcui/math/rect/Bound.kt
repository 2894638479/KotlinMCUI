package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px


interface Bound {
    val low: Measure
    val high: Measure
    companion object {
        val empty = object : Bound {
            override val low get() = 0.px
            override val high get() = 0.px
        }
    }
}

fun Bound(low: Measure,high: Measure):Bound = BoundImpl(low,high)

inline val Bound.range get() = low..high
inline val Bound.isEmpty get() = high <= low
inline fun Bound.ifEmpty(action: () -> Bound) = if(isEmpty) action() else this
fun Bound.contains(value: Measure) = value in range
fun Bound.expand(low: Measure = 0.px,high: Measure = 0.px) = object :Bound {
    override val low get() = this@expand.low - low
    override val high get() = this@expand.high + high
}
