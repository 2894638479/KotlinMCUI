package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Position

/**
 * A stack of composed [Transform] matrices for rendering.
 *
 * Each [push] composes a new transform onto the stack top. [pop] restores
 * the previous state. Use [transform] to apply the current composed matrix
 * to a position before passing vertices to the backend.
 */
class TransformStack {
    private val stack = ArrayDeque<Transform>()

    /** The current composed transform (stack top), or [Transform.empty] if empty. */
    val current get() = stack.lastOrNull() ?: Transform.empty

    /** Push and compose [t] onto the stack. */
    fun push(t: Transform) {
        stack.addLast(current * t)
    }

    /** Remove the top of the stack. */
    fun pop() {
        stack.removeLast()
    }

    /** Execute [block] with [t] pushed, then pop. */
    inline fun with(t: Transform, block: () -> Unit) {
        push(t)
        try { block() } finally { pop() }
    }

    /** Apply the current composed transform to [pos]. */
    fun transform(pos: Position): Position = current * pos
}
