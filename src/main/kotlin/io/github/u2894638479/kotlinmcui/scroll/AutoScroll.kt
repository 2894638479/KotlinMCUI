package io.github.u2894638479.kotlinmcui.scroll

import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.math.px

/**
 * Automatic scroll-to-focus behaviour for scrollable containers.
 * @param sources Which focus change triggers scrolling.
 * @param constraint How the focused item is positioned in the viewport.
 *                   Defaults to [ScrollConstraint.KeepIn]\(0.px\) — keep in view with no padding.
 */
class AutoScroll(
    val sources: Set<FocusSource>,
    val constraint: ScrollConstraint = ScrollConstraint.KeepIn(0.px),
)
