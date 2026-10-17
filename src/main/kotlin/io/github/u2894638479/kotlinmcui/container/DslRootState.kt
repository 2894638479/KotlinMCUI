package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.PreeditInfo

/**
 * The view state owned by one root: which component is focused, which one is
 * hovered, and the current IME preedit.
 *
 * The three fields are mutable, but every write goes through a method below,
 * which is where the state transition and its notification live. The state used
 * to be stored directly on the process-wide [TopComponent], which made "each
 * output has its own focus and hover" impossible: the slots were shared by every
 * root. [TopComponent] now holds the main root's instance and projects it.
 *
 * Broadcasting a change means walking the tree that owns this state, so the owner
 * passes the two callbacks in: this class only knows *that* a change happened.
 *
 * The name is deliberate. The next step publishes this as `DslRootState`, the
 * per-root read-only state view (the fields read as `val`, the mutators kept beside
 * the root), rather than introducing a second type that would have to be reconciled
 * with this one later.
 */
internal class DslRootState(
    private val onFocusChanged: (DslId?, FocusSource) -> Unit,
    private val onHoverChanged: (DslId?) -> Unit
) {
    var focused: DslId? = null
        private set

    var hovered: DslId? = null
        private set

    var preedit = PreeditInfo.empty
        private set

    /** Move the focus, then broadcast the change; `null` clears it. */
    fun setFocus(newFocus: DslId?, source: FocusSource = FocusSource.CODE) {
        focused = newFocus
        onFocusChanged(newFocus, source)
    }

    /**
     * Record [value] as the hovered identity.
     *
     * The notification is unconditional, matching the render pass, which rewrites
     * the hover every frame rather than only when the identity changes.
     */
    fun setHovered(value: DslId?) {
        hovered = value
        onHoverChanged(value)
    }

    /** Record the IME preedit state. Unlike focus and hover, this carries no notification. */
    fun setPreedit(info: PreeditInfo) {
        preedit = info
    }
}
