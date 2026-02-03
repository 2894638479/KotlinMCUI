package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

interface Alignable {
    val minSize: Measure
    val size: Measure
    val weight: Double
    var low: Measure
    var high: Measure
    val align: Align
}

val Alignable.defaultSize get() = size.ifNan { minSize.ifNan { 0.px } }
val Alignable.defaultSizePx get() = size.pixelsOrElse { minSize.pixelsOrElse { 0.0 } }