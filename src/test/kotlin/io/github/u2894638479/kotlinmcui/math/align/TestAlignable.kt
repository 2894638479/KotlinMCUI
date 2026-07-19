package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

/**
 * Minimal test implementation of [Alignable] for testing aligner strategies.
 */
class TestAlignable(
    override var low: Measure = 0.px,
    override var high: Measure = 0.px,
    override val minSize: Measure = 0.px,
    override val size: Measure = Measure.AUTO,
    override val weight: Double = 1.0,
    override val align: Align = Align.LOW
) : Alignable
