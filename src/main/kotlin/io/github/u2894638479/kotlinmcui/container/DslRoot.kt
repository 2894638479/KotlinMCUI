package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.backend.dslBackend
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.entry.DslEntryLoader

/**
 * One root: the state, the viewport and the children that belong to it.
 *
 * [TopComponent] stands for the main root and delegates to an instance of this, so that object is a
 * projection of a root instead of the owner of the process-wide state. Its children are built on
 * first use: the root-level overlay screen is collected first, and whatever the backend mounts
 * follows it.
 *
 * The name is deliberate: this is the early form of the `DslRoot` the design draft declares, and it
 * grows in place - `provider`, `mount`/`unmount`, `advanceFrame` and the rest are added here rather
 * than in a second type that would have to be reconciled with this one later.
 */
internal class DslRoot(
    val state: DslRootState,
    val geometry: DslRootGeometry
) {
    private val rawChildren: DslChild.Group by lazy {
        DslChild.Group().also {
            it.collect(dslBackend.create("Dsl Overlay") { DslEntryLoader.overlays() }.dslScreen)
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
