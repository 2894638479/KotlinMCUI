package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenSumWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl

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