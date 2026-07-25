package io.github.u2894638479.kotlinmcui.math

/** How to scroll a focused item into the viewport. */
enum class ScrollConstraint {
    /** If already fully visible, do nothing. If above, snap top. If below, snap bottom. */
    MINIMAL,
    /** Always center the focused item in the viewport. */
    CENTER,
}
