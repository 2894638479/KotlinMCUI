package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutBound

/**
 * An element that participates in alignment layout.
 */
interface Alignable: MutBound {
    /** Minimum size the element needs. */
    val minSize: Measure
    /** Desired size; [Measure.AUTO] means "determine from content". */
    val size: Measure
    /** Layout weight for distribution of remaining space. */
    val weight: Double
    override var low: Measure
    override var high: Measure
    /** Where the element should align within its allocated space. */
    val align: Align
}

val Alignable.autoSizeMin get() = size.ifNan { minSize.ifNan { 0.px } }
fun Alignable.autoSize(default:Measure): Measure {
    val size = size
    return if(size.isAutoMin) minSize.ifNan { 0.px }
    else size.ifNan { default }
}