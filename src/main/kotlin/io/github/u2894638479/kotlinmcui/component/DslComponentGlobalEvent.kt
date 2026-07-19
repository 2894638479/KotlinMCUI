package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId

/**
 * Global state change notifications broadcast to all components.
 *
 * These are called whenever the focused or hovered component changes anywhere
 * in the tree, allowing components to react (e.g. for animations or styling).
 */
interface DslComponentGlobalEvent {
    /**
     * The globally focused component has changed.
     * @param newFocus The identity of the newly focused component, or `null`.
     */
    fun globalFocusChanged(newFocus: DslId?) {}

    /**
     * The globally hovered component has changed.
     * @param newHover The identity of the newly hovered component, or `null`.
     */
    fun globalHoverChanged(newHover: DslId?) {}
}
