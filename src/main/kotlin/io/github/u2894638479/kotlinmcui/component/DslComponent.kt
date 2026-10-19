package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.contains

/**
 * The core interface for all DSL components in the UI tree.
 *
 * A component encapsulates layout, rendering, event handling, and child management.
 * It combines multiple specialised interfaces:
 * - [DslComponentNavigator] — focus traversal views
 * - [DslComponentAlign] — alignment and layout measurement
 * - [DslComponentEvent] — input event handling (mouse, keyboard, scroll)
 * - [DslComponentGlobalEvent] — global state change notifications (focus, hover)
 * - [DslComponentMetadata] — identity, modifiers, accessibility properties
 * - [DslIdContext] / [DslScaleContext] — contextual identity and scale
 *
 * Most methods have default no-op implementations; override only what you need.
 */
interface DslComponent: DslComponentNavigator, DslComponentAlign, DslComponentEvent, DslComponentGlobalEvent, DslComponentMetadata,
    DslIdContext, DslScaleContext {
    /** Child components of this component. Empty by default. */
    val children: DslChild.Group get() = DslChild.Group.empty

    /**
     * The store of the tree this component belongs to, or `null` while that link is not wired.
     *
     * This is how a component finds its own tree: focus and hover are read through the store's root
     * rather than from a process-wide slot. A component whose store has no root is not part of a
     * mounted tree, and reading [isFocused] or [isHovered] on it fails loudly instead of guessing.
     */
    val dataStore: DslDataStore? get() = null

    /** Build children and initialise state. Called once before layout. */
    fun build() {}

    /**
     * Perform horizontal layout for this component and its children.
     *
     * ### Layout order
     * The framework always runs all horizontal layout first, then all vertical
     * layout. This fixed order allows a component's vertical layout to depend
     * on its children's horizontal layout results (e.g. text wrapping needs to
     * know the available width before computing line heights).
     */
    fun layoutHorizontal() {}

    /** Perform vertical layout for this component and its children. */
    fun layoutVertical() {}

    override fun <T> testHit(mouse: Position, get: (DslComponent) -> T?) =
        if(mouse in instance.rect) get(instance) else null

    override fun <T> testHit(get: (DslComponent) -> T?) = get(instance)

    override val viewHorizontal get() = listOf<List<DslComponent>>()
    override val viewVertical get() = listOf<List<DslComponent>>()
    override val viewSequential get() = listOf<DslComponent>()
}

/**
 * The state of the tree this component belongs to.
 *
 * A component that is not part of a mounted tree has no tree to answer from. Answering "not focused"
 * would hide the mistake, so this fails loudly instead - the error names the component and the cause.
 */
private val DslComponent.treeState: DslRootState
    get() = dataStore?.rootState ?: error(
        "${this::class.simpleName ?: "this component"} is not mounted on any root: " +
            "its screen's store has no root state. Mount the screen on a root before reading focus or hover."
    )

/**
 * Whether this component currently has keyboard focus.
 *
 * A component of a tree that is not mounted on any root fails loudly rather than reporting `false`:
 * the question has no answer there, and a guess would hide the real mistake.
 */
val DslComponent.isFocused get() = focusable && treeState.focused == identity

/**
 * Whether the mouse is currently hovering over this component.
 *
 * Fails loudly when the component belongs to no mounted tree, for the same reason as [isFocused].
 */
val DslComponent.isHovered get() = treeState.hovered == identity

/**
 * Whether this component is visually highlighted (focused or hovered).
 */
val DslComponent.isHighlighted get() = highlightable && (isFocused || isHovered)

/** Attach this component as its own [DslComponentMetadata.instance]. */
internal fun DslComponent.attachInstance() { instance = this }
