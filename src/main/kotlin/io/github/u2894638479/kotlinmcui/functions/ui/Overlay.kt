package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.align.OverlayAlign
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl


context(ctx: DslContext)
fun Overlay(
    modifier: Modifier = Modifier,
    align: OverlayAlign,
    id: Any? = null,
    content: DslFunction
) {
    val delegate = DslScopeImpl(DslId(id ?: content::class),modifier,ctx,content)
    ctx.overlays.collect(
        object : DslComponent by delegate {
            override val contentMinWidth by lazy {
                max(instance.childrenMaxWidth,super.contentMinWidth)
            }
            override val contentMinHeight by lazy {
                max(instance.childrenMaxHeight,super.contentMinHeight)
            }
            override fun layoutHorizontal() {
                align.layoutAxis(instance, modifier, Axis.Horizontal)
                delegate.layoutHorizontal()
            }
            override fun layoutVertical() {
                align.layoutAxis(instance, modifier, Axis.Vertical)
                delegate.layoutVertical()
            }
        }
    )
}

context(ctx: DslContext)
fun DslChild.Overlay(
    modifier: Modifier = Modifier,
    id:Any? = null,
    content: DslFunction
) = apply { Overlay(modifier, OverlayAlign.FixRect(currentComponent().rect),id,content) }