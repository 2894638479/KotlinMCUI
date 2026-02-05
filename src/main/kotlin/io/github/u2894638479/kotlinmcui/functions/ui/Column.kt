package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.scope.childrenSumHeight

context(ctx: DslContext)
fun Column(
    modifier: Modifier = Modifier,
    alignerVertical: Aligner = Aligner.weightedStrictByMin,
    id:Any? = null,
    function: DslFunction
) = collect(
    object : DslScope by DslScopeImpl(
        newChildId(id ?: function::class),
        modifier,
        ctx,
        function,
        alignerVertical = alignerVertical
    ) {
        val lazyWidth by lazy { childrenMaxWidth }

        context(instance: DslComponent)
        override val contentMinWidth get() = Measure.max(lazyWidth, super.contentMinWidth)

        val lazyHeight by lazy { childrenSumHeight }

        context(instance: DslComponent)
        override val contentMinHeight get() = Measure.max(lazyHeight, super.contentMinHeight)

        override val viewHorizontal get() = listOf(children)
        override val viewVertical get() = children.mapView { listOf(it) }
    }
)