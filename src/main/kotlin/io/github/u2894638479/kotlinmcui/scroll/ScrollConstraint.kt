package io.github.u2894638479.kotlinmcui.scroll

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align

/** How to scroll a focused item into the viewport. */
sealed interface ScrollConstraint {
    /** Keep the item within the viewport with at least [padding] inset on each side. */
    data class KeepIn(val padding: Measure) : ScrollConstraint

    /** Like [KeepIn], but [ratio] is a fraction of the viewport size (0.1 = 10%). */
    data class KeepInRatio(val ratio: Double) : ScrollConstraint

    /** Always scroll the item to [align] in the viewport. */
    data class AlignTo(val align: Align) : ScrollConstraint
}
