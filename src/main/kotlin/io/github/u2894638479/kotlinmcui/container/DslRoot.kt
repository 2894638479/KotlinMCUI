package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRootInput
import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.DslRootScreenFactory
import io.github.u2894638479.kotlinmcui.backend.DslRootUtils
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.prop.PropertyLifeScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * One root: the state, the viewport and the children that belong to it.
 *
 * The backend builds it and keeps it; nothing in the runtime hands roots out. A root is self-sufficient:
 * it takes the four platform pieces it draws with, owns its own state and its viewport, and never looks
 * anything up globally.
 *
 * It does not render itself. Each frame the owner writes [rect] and then, inside the render context its
 * platform gives it, calls `component.render()` - those two calls together are "one frame of this
 * root", and the order between them is the contract because the render pass copies [rect] onto every
 * mounted screen.
 */
class DslRoot(
    /** Pointer and keyboard state, and the preedit area the platform should show. */
    val input: DslRootInput,
    /** Drawing, or `null` for a root that draws nothing - in which case there is not even a font. */
    val renderer: DslRootRenderer<*>?,
    /** Creates the screens mounted on this root, or `null` when this root cannot hold screens. */
    val screenFactory: DslRootScreenFactory<*>?,
    /** The per-output helpers: clipboard, narration, sounds, capture, quit. */
    val rootUtils: DslRootUtils,
) {
    private val rootComponent = DslRootComponent(this)

    /**
     * Coroutines started here are cancelled when this root closes.
     *
     * One scope per root, on the root's own thread: it is the session's lifetime, which is the lifetime
     * of the root. A concurrent coroutine that reads or writes the tree uses this scope, so it cannot
     * outlive the tree it touches.
     */
    val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + rootUtils.sessionDispatcher)

    /**
     * Run [block] on this root's thread, inline when the caller is already on it.
     *
     * [sessionScope] is what makes it end with the root: a block queued after `close()` never runs.
     * Across threads the only ordering is the dispatcher's queue, so it is not a way to sequence work
     * against the frame loop.
     */
    fun post(block: () -> Unit) {
        if(rootUtils.isOnSessionThread) block()
        else sessionScope.launch { block() }
    }

    /**
     * The root's own state.
     *
     * Its callbacks are wired here, not by the caller: the root knows its component, so nobody outside
     * has to break the cycle between the state and the tree it belongs to.
     */
    internal val state: DslRootState = DslRootState(
        onFocusChanged = { focus, source -> rootComponent.globalFocusChanged(focus, source) },
        onHoverChanged = { hovered -> rootComponent.globalHoverChanged(hovered) },
        onNarrate = { text -> rootUtils.narrate(text) },
    )

    /**
     * The component that represents this root in the tree.
     *
     * Stable for the root's whole lifetime: the tree walks it, the focus broadcast enters it and the
     * backend drives it. Rebuilding it would break the `it !== this` checks and the focused identity.
     */
    val component: DslScope get() = rootComponent

    /**
     * Whether the component that currently holds the focus accepts IME input - that is, whether its
     * preedit area is non-`null`.
     *
     * The platform asks the root this question, because it needs to know whether text input belongs
     * to the game or to a field on screen. It lives here rather than on [DslScope] on purpose: that
     * would ask every container the same question and every implementation would have to answer it.
     */
    val preeditUsing: Boolean get() = rootComponent.preeditUsing

    /**
     * This root's viewport.
     *
     * The owner owns it: each frame it sets the rect to the full output size and then renders
     * [component]. The render pass copies this rect onto every mounted screen.
     */
    val rect: MutRect = MutRect()
    /**
     * Scratch buffers for the triangle swatches of this root's colour pickers.
     *
     * One pair **per root**, not one pair for the process: the arrays are filled and consumed inside a
     * single draw call, and two roots may be rendering on two threads at the same moment. Sharing one
     * pair would let one root's fill land between another's fill and its `renderTriangles` call.
     */
    internal val triangleVertices = FloatArray(6)
    internal val triangleColors = IntArray(3)

    /**
     * The session slots of this root: keyed by the bare key, shared by every screen mounted here.
     *
     * Concurrent for the same reason the process-wide static map is: one process may render several
     * roots on different threads, and every access is a single-key map operation. [close] runs each
     * slot's cleanup and empties the map - that is what "released with the root" means.
     */
    internal val sessionPropertyMap = java.util.concurrent.ConcurrentHashMap<DslId, PropertyLifeScope.TimedProperty<*>>()

    private var closed = false

    /** Whether [close] has run. */
    val isClosed: Boolean get() = closed

    /**
     * Close this root: idempotent, and from here on every way of *using* it fails loudly.
     *
     * The order is **close first, then stop your own driving** - the frame loop, the executor, the
     * recorder. This door deliberately does not stop those things: they belong to the backend, which is
     * the only one that knows what it started. What the door guarantees is that a frame arriving after
     * the close fails here, in the right place, instead of drawing into a root nobody is looking at.
     *
     * Calling it twice is fine.
     */
    fun close() {
        if(closed) return
        closed = true
        sessionScope.cancel()
        sessionPropertyMap.values.forEach { it.onClear() }
        sessionPropertyMap.clear()
    }

    internal fun checkOpen(what: String) {
        check(!closed) { "this root is closed: ${what} is not available after DslRoot.close()" }
    }
    private val rawChildren: DslChild.Group by lazy {
        val factory = screenFactory
        require(factory != null) { "this root cannot hold screens: it was built without a screen factory" }
        DslChild.Group().also {
            it.collect(factory.create("Dsl Overlay") { DslEntryLoader.overlays() }.dslScreen)
        }
    }

    /**
     * This root's children: the overlay screen first, then whatever is mounted on it.
     *
     * Mounting stamps the mounted screen's store with this root's state, so the components that screen
     * builds find their own tree through `dataStore.rootState` rather than guessing a global root.
     * The stamp is written on **every** [DslChild.Group.collect], not only the first one: writing it
     * again is a no-op for a screen that comes back to the same root, and re-writing it is the only
     * thing that stops a screen taken off one root from answering with that root's state once it is
     * mounted on another.
     *
     * Removing does not clear the stamp. A hidden screen is not a destroyed one, and it is still the
     * screen this root last mounted; the stamp lives on the screen's store, so it survives the child
     * wrapper being thrown away.
     *
     * The viewport is deliberately **not** stamped here. The owner writes [rect] every frame, and that
     * single write is the truth for the rect - a second write at mounting time would be a second truth.
     */
    val children: DslChild.Group by lazy {
        object : DslChild.Group, List<DslComponent> by rawChildren {
            override fun collect(child: DslComponent) = checkOpen("mounting a screen").let { rawChildren.collect(child) }.also {
                (child as? DslScreen)?.let { screen -> screen.dataStore.rootState = state }
            }

            override fun collectAll(list: List<DslComponent>) {
                list.forEach { collect(it) }
            }

            override fun remove(child: DslChild) = checkOpen("unmounting a screen").let { rawChildren.remove(child) }
        }
    }

    /**
     * Move the focus of this root to [id], or clear it with `null`.
     *
     * This is the entry for code that has to put the cursor somewhere as a page opens - the mouse and
     * the keyboard drive the same function internally. It is deliberately the *only* new public member:
     * [state] stays internal, so `setHovered`, `setPreedit` and the frame counters are not part of the
     * public surface just because focus had to be.
     *
     * It does not filter by `focusable`: a target that cannot take focus is still recorded, and its
     * `isFocused` stays `false` - "the root knows the focus" and "the component reports it" are two
     * different facts.
     */
    fun setFocus(id: DslId?, source: FocusSource = FocusSource.CODE) {
        checkOpen("setting the focus")
        state.setFocus(id, source)
    }
}
