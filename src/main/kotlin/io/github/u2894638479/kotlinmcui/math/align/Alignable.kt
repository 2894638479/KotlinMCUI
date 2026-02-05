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

val Alignable.autoSizeMin get() = size.ifNan { minSize.ifNan { 0.px } }
fun Alignable.autoSize(default:Measure): Measure {
    val size = size
    return if(size.isAutoMin) minSize.ifNan { 0.px }
    else size.ifNan { default }
}