package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.component.childrenSumHeight
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

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
        override val contentMinWidth by lazy {
            max(instance.childrenMaxWidth,super.contentMinWidth)
        }

        override val contentMinHeight by lazy {
            max(instance.childrenSumHeight,super.contentMinHeight)
        }

        override val viewHorizontal get() = listOf(children)
        override val viewVertical get() = children.mapView { listOf(it) }
    }
)