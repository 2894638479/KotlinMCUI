package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure


interface MutBound: Bound {
    override var low: Measure
    override var high: Measure
}

fun MutBound(low: Measure,high: Measure): MutBound = BoundImpl(low,high)

