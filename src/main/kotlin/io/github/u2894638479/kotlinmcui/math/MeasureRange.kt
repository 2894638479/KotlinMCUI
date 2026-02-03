package io.github.u2894638479.kotlinmcui.math

class MeasureRange(
    override val start: Measure,
    override val endInclusive: Measure
) :ClosedFloatingPointRange<Measure> {
    override fun lessThanOrEquals(a: Measure, b: Measure) = a <= b
}