package io.github.u2894638479.kotlinmcui.math

/** How to scroll a focused item into the viewport. */
enum class ScrollConstraint {
    /** If already fully visible, do nothing. If above, snap top. If below, snap bottom. */
    MINIMAL,
    /** Scroll the item to the top of the viewport. */
    LOW,
    /** Scroll the item to the center of the viewport. */
    CENTER,
    /** Scroll the item to the bottom of the viewport. */
    HIGH,
}
