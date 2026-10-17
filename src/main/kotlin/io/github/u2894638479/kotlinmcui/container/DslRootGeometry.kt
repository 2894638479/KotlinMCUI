package io.github.u2894638479.kotlinmcui.container

import io.github.u2894638479.kotlinmcui.math.rect.MutRect

/**
 * The viewport of one root.
 *
 * Kept out of [DslRootState] on purpose. That container is the root's state, and every field in it
 * has a single write path through a method; this is one mutable object the backend writes in place
 * every frame instead. The split also matches where the fields end up: the viewport belongs to the
 * root itself, while [DslRootState] grows into the public read-only view of the state.
 */
internal class DslRootGeometry {
    /** The full screen rect, which the backend must set each frame. */
    val rect = MutRect()
}
