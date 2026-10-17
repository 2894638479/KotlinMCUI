package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslBackendInput
import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.DslRootScreenFactory
import io.github.u2894638479.kotlinmcui.backend.DslRootUtils
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.math.rect.MutRect

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
    val input: DslBackendInput,
    /** Drawing, or `null` for a root that draws nothing - in which case there is not even a font. */
    val renderer: DslBackendRenderer<*>?,
    /** Creates the screens mounted on this root, or `null` when this root cannot hold screens. */
    val screenFactory: DslRootScreenFactory<*>?,
    /** The per-output helpers: clipboard, narration, sounds, capture, quit. */
    val rootUtils: DslRootUtils,
) {
    private val rootComponent = DslRootComponent(this)

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
            override fun collect(child: DslComponent) = rawChildren.collect(child).also {
                (child as? DslScreen)?.let { screen -> screen.dataStore.rootState = state }
            }

            override fun collectAll(list: List<DslComponent>) {
                list.forEach { collect(it) }
            }

            override fun remove(child: DslChild) = rawChildren.remove(child)
        }
    }
}
