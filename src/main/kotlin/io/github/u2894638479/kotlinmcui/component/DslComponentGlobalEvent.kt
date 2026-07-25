package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.PreeditInfo

/**
 * Global state change notifications dispatched from the root of the tree.
 *
 * Unlike [io.github.u2894638479.kotlinmcui.component.DslComponentEvent] methods
 * which propagate parent-to-child, global events are dispatched by
 * [io.github.u2894638479.kotlinmcui.container.TopComponent] — they may target
 * a specific component (e.g. only the focused component receives
 * [globalPreedit] when preedit is active) or be sent to the entire tree
 * (e.g. [globalPreedit] with empty text to clear state).
 */
interface DslComponentGlobalEvent {
    /**
     * The globally focused component has changed.
     * @param newFocus The identity of the newly focused component, or `null`.
     * @param source What triggered the change ([FocusSource.KEYBOARD], [FocusSource.MOUSE], or [FocusSource.CODE]).
     */
    fun globalFocusChanged(newFocus: DslId?, source: FocusSource) {}

    /**
     * The globally hovered component has changed.
     * @param newHover The identity of the newly hovered component, or `null`.
     */
    fun globalHoverChanged(newHover: DslId?) {}

    /**
     * IME preedit (composition) state has changed.
     *
     * When [info] is non-empty, this is dispatched only to the currently
     * focused component. When [info] is [PreeditInfo.empty], it is dispatched
     * to all components to signal that composition has ended.
     */
    fun globalPreedit(info: PreeditInfo) {}
}
