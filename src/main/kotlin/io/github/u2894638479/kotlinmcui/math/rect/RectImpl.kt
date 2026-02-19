package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

class RectImpl(
    override var left: Measure = 0.px,
    override var top: Measure = 0.px,
    override var right: Measure = 0.px,
    override var bottom: Measure = 0.px,
): MutRect