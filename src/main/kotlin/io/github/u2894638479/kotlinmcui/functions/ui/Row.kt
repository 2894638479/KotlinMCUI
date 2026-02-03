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
import io.github.u2894638479.kotlinmcui.scope.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.scope.childrenSumWidth

context(ctx: DslContext)
fun Row(modifier: Modifier = Modifier, alignerHorizontal: Aligner = Aligner.weightedStrictByMin, id:Any? = null, function: DslFunction) =
    collect(
        object : DslScope by DslScopeImpl(
            newChildId(id ?: function::class),
            modifier,
            ctx,
            function,
            alignerHorizontal = alignerHorizontal
        ) {
            val lazyWidth by lazy { childrenSumWidth }

            context(instance: DslComponent)
            override val contentMinWidth get() = Measure.max(lazyWidth, super.contentMinWidth)

            val lazyHeight by lazy { childrenMaxHeight }

            context(instance: DslComponent)
            override val contentMinHeight get() = Measure.max(lazyHeight, super.contentMinHeight)

            override val viewHorizontal get() = children.mapView { listOf(it) }
            override val viewVertical get() = listOf(children)
        }
    )