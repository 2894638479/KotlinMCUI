package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.PreeditInfo

/**
 * The view state owned by one root: which component is focused, which one is hovered, the current
 * IME preedit, what was last announced to a screen reader, and how the frame clock and the last
 * render failure are doing.
 *
 * The fields are mutable, but every write goes through a method below, which is where the state
 * transition and its notification live. The state used to live in one process-wide slot, which made
 * "each output has its own focus and hover" impossible; every root now owns an instance of this.
 *
 * Broadcasting a change means walking the tree that owns this state, so the owner passes the
 * callbacks in: this class only knows *that* a change happened.
 *
 * The name is deliberate. The next step publishes this as `DslRootState`, the per-root read-only
 * state view (the fields read as `val`, the mutators kept beside the root), rather than introducing
 * a second type that would have to be reconciled with this one later. Its field set is already the
 * one that view declares, which is why the root's viewport lives in [DslRootGeometry] instead.
 */
internal class DslRootState(
    private val onFocusChanged: (DslId?, FocusSource) -> Unit,
    private val onHoverChanged: (DslId?) -> Unit,
    private val onNarrate: (String) -> Unit
) {
    var focused: DslId? = null
        private set

    var hovered: DslId? = null
        private set

    var preedit = PreeditInfo.empty
        private set

    /**
     * How many frames the platform has rendered through the framework. It stays zero until the game
     * is past its loading overlay, because the backend does not call in before that - which makes
     * this both "the platform can show a screen now" and the frame clock a preview session waits on.
     */
    @Volatile
    var frameIndex = 0L
        private set

    /** The last exception a page threw while rendering, kept so a preview session can report it. */
    @Volatile
    var renderFailure: Throwable? = null
        private set

    /** The last accessibility text the keyboard path announced, or `null`. */
    var keyboardNarration: String? = null
        private set

    /** The last accessibility text the pointer path announced, or `null`. */
    var mouseNarration: String? = null
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

    /** Move the frame clock on by one frame. */
    fun advanceFrame() {
        frameIndex++
    }

    /** Record the exception a page threw while rendering; `null` clears it. */
    fun noteRenderFailure(failure: Throwable?) {
        renderFailure = failure
    }

    /**
     * Announce [text], unless it is what this path announced last: repeating the same sentence is
     * not news to a screen reader. `null` clears the record without announcing anything.
     */
    fun noteKeyboardNarration(text: String?) {
        if(text != keyboardNarration) {
            keyboardNarration = text
            text?.let(onNarrate)
        }
    }

    /** The same rule as [noteKeyboardNarration], for the pointer path. */
    fun noteMouseNarration(text: String?) {
        if(text != mouseNarration) {
            mouseNarration = text
            text?.let(onNarrate)
        }
    }
}
