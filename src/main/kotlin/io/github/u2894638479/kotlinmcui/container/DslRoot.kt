package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.DslRootProvider
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader
import io.github.u2894638479.kotlinmcui.math.rect.MutRect

/**
 * One root: the state, the viewport and the children that belong to it.
 *
 * The process's main root is one of these, and the runtime hands it out as the owner root. Its
 * children are built on first use: the root-level overlay screen is collected first, and whatever the
 * backend mounts follows it.
 *
 * The name is deliberate: this is the early form of the `DslRoot` the design draft declares, and it
 * grows in place - `provider`, `mount`/`unmount`, `advanceFrame` and the rest are added here rather
 * than in a second type that would have to be reconciled with this one later. Only the members a
 * backend needs today are public; the rest stays internal until the draft's own shape grows here.
 */
class DslRoot internal constructor(
    internal val state: DslRootState,
    internal val geometry: DslRootGeometry,
    /**
     * The platform pieces this root draws, reads input and creates screens with.
     *
     * Captured when the root is built rather than looked up globally afterwards. Note the ordering that
     * pages depend on: [state] must be usable before any screen is built, because a screen's store is
     * stamped with it when the screen is mounted, and a page may read focus while it builds.
     */
    internal val provider: DslRootProvider<*, *>,
    /** How long this root is expected to live; see [ViewerLifetime]. */
    val viewerLifetime: ViewerLifetime = ViewerLifetime.NONE,
    /** Whether this is the one root that owns the process. Only `createOwner` sets it. */
    val isOwner: Boolean = false,
    /**
     * Why this root was refused, or `null` for a working root.
     *
     * A refused root is a real object on purpose: the caller gets something that fails loudly and
     * carries its own reason, instead of a `null` it might forget to check.
     */
    internal val refusal: io.github.u2894638479.kotlinmcui.backend.DslRuntime.DslRejectionReason? = null
) {
    private val rootComponent = DslRootComponent(this)

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
     * The backend owns it: each frame it sets the rect to the full screen size and then renders
     * [component]. Those two calls together are "one frame of this root"; the order between them is
     * the contract, because the render pass copies this rect onto every mounted screen.
     */
    val rect: MutRect get() = geometry.rect

    private val rawChildren: DslChild.Group by lazy {
        refusal?.let { error("this root was refused: $it") }
        val factory = provider.screenFactory ?: error("this root's provider cannot create screens")
        DslChild.Group().also {
            it.collect(factory.create("Dsl Overlay") { DslEntryLoader.overlays() }.dslScreen)
        }
    }

    /**
     * This root's children: the overlay screen first, then whatever is mounted on it.
     *
     * Mounting stamps the mounted screen's store with this root's state, so the components that screen
     * builds find their own tree through `dataStore.rootState` rather than guessing the main root.
     * The stamp is written on **every** [DslChild.Group.collect], not only the first one: writing it
     * again is a no-op for a screen that comes back to the same root, and re-writing it is the only
     * thing that stops a screen taken off one root from answering with that root's state once it is
     * mounted on another.
     *
     * Removing does not clear the stamp. A hidden screen is not a destroyed one, and it is still the
     * screen this root last mounted; the stamp lives on the screen's store, so it survives the child
     * wrapper being thrown away.
     *
     * The viewport is deliberately **not** stamped here. The root writes [geometry] every frame, and
     * that single write is the truth for the rect - a second write at mounting time would be a second
     * truth.
     *
     * Callers are the backend's screen lifecycle, which runs on the thread that renders this root, so
     * the stamp is written on the thread this root's state and children belong to. Nothing here
     * synchronises because that thread is the only writer.
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
