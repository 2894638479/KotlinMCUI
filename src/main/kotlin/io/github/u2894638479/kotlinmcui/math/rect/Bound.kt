package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Align.*
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
fun Bound(align: Align, pos: Measure, size: Measure) = when(align) {
    LOW -> Bound(pos - size, pos)
    MID -> Bound(pos - size/2, pos + size/2)
    HIGH -> Bound(pos, pos + size)
}
fun Bound.alignIn(align: Align, size: Measure) = when(align) {
    LOW -> Bound(low, low + size)
    MID -> Bound(center - size/2, center + size/2)
    HIGH -> Bound(high - size, high)
}
fun Bound.alignOut(align: Align, size: Measure) = when(align) {
    LOW -> Bound(low - size, low)
    MID -> Bound(center - size/2, center + size/2)
    HIGH -> Bound(high, high + size)
}

inline val Bound.range get() = low..high
inline val Bound.isEmpty get() = high <= low
inline val Bound.size get() = high - low
inline val Bound.center get() = (low + high) / 2
inline fun Bound.ifEmpty(action: () -> Bound) = if(isEmpty) action() else this
fun Bound.contains(value: Measure) = value in range
fun Bound.expand(low: Measure = 0.px,high: Measure = 0.px) = object :Bound {
    override val low get() = this@expand.low - low
    override val high get() = this@expand.high + high
}
