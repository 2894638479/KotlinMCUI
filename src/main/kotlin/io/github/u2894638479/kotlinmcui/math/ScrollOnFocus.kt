package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.component.FocusSource

/**
 * Automatic scroll-to-focus behaviour for scrollable containers.
 * @param sources Which focus change triggers scrolling.
 * @param constraint How the focused item is positioned in the viewport.
 */
class ScrollOnFocus(
    val sources: Set<FocusSource>,
    val constraint: ScrollConstraint = ScrollConstraint.MINIMAL,
)
