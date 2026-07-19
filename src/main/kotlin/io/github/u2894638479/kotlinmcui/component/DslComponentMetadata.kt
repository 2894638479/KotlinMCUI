package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * Metadata and accessibility properties of a component.
 *
 * Includes the component's [modifier], the self-referencing [instance] (used
 * by the decorator pattern), the visual [transform], and accessibility /
 * behaviour flags.
 */
interface DslComponentMetadata {
    /** The modifier chain controlling size, padding, and weight. */
    val modifier: Modifier

    /**
     * A self-reference to the outermost wrapper of this component.
     * In the decorator pattern (`object : DslComponent by inner { ... }`),
     * [instance] refers to the decorator while the delegate refers to the inner.
     */
    var instance: DslComponent

    /**
     * The transform of this component **relative to its parent**.
     *
     * During rendering and event propagation, transforms are composed layer by
     * layer from parent to child. Since most components have no transform, the
     * default is [Transform.empty], allowing the composition chain to short-circuit
     * (identity × anything = anything) for zero overhead in the common case.
     *
     * Set a non-empty transform only when this component needs to be rotated,
     * scaled, or translated relative to its parent.
     */
    val transform: Transform get() = Transform.empty

    /** Whether this component can receive keyboard focus. */
    val focusable: Boolean get() = false

    /** An optional tooltip DSL function. */
    val tooltip: DslFunction? get() = null

    /** The accessibility narration text for screen readers. */
    val narration: String? get() = null

    /** Whether this component provides accessibility narration. */
    val narratable: Boolean get() = false

    /** Whether this component can be visually highlighted. Defaults to [focusable]. */
    val highlightable: Boolean get() = instance.focusable
}
