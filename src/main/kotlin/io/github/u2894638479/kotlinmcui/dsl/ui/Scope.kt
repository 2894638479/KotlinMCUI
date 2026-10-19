package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * Create a container component whose children are placed by [alignerHorizontal]
 * and [alignerVertical].
 *
 * [Box], [Row] and [Column] are the common cases; use this one to build a container
 * with a different space distribution, or as the base of a custom container:
 * ```
 * Scope(modifier, id, Aligner.close(Align.MID), Aligner.weightedStrictByMin, function)
 *     .change { object : DslScope by it as DslScope { ... } }
 * ```
 *
 * @param id Unique identifier; see [Box] for the default rule.
 * @param alignerHorizontal Distributes the children along the horizontal axis.
 * @param alignerVertical Distributes the children along the vertical axis.
 * @param function DSL builder function executed within an implicit [DslContext].
 */
context(ctx: DslContext)
fun Scope(
    modifier: Modifier = Modifier,
    id: Any? = null,
    alignerHorizontal: Aligner = Aligner.simplePlace,
    alignerVertical: Aligner = Aligner.simplePlace,
    function: DslFunction
) = collect(
    DslScopeImpl(
        newChildId(id ?: function::class),
        modifier, ctx, function, alignerHorizontal, alignerVertical
    )
)
