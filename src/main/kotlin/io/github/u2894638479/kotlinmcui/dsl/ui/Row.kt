package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenSumWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.util.mapView
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

/**
 * Create a horizontal layout container (Row).
 *
 * Row arranges children along the horizontal axis with a configurable aligner.
 * Its `contentMinWidth` is the maximum of children's total width (`childrenSumWidth`)
 * and the modifier constraint; `contentMinHeight` is the maximum of children's
 * maximum height (`childrenMaxHeight`) and the modifier constraint.
 *
 * Overrides [DslScope]'s `viewHorizontal` and `viewVertical`:
 * - `viewHorizontal`: each child forms a single-row view (for per-column measurement)
 * - `viewVertical`: all children as a single column view
 *
 * @param alignerHorizontal The horizontal alignment strategy for distributing space.
 *                          Defaults to [Aligner.weightedStrictByMin].
 * @param id Unique identifier; see [Box] for the default rule.
 * @param function DSL builder function for declaring children.
 * @receiver [DslContext] The implicit DSL context.
 * @see DslScopeImpl
 * @see Aligner
 */
context(ctx: DslContext)
fun Row(
    modifier: Modifier = Modifier,
    alignerHorizontal: Aligner = Aligner.weightedStrictByMin,
    id:Any? = null,
    function: DslFunction
) = collect(
    object : DslScope by DslScopeImpl(
        newChildId(id ?: function::class),
        modifier,
        ctx,
        function,
        alignerHorizontal = alignerHorizontal
    ) {
        override val contentMinWidth by lazy {
            max(instance.childrenSumWidth,super.contentMinWidth)
        }

        override val contentMinHeight by lazy {
            max(instance.childrenMaxHeight,super.contentMinHeight)
        }

        override val viewHorizontal get() = children.mapView { listOf(it) }
        override val viewVertical get() = listOf(children)
    }
)