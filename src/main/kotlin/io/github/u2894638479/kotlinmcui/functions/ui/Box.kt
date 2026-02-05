package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.remove
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl
import io.github.u2894638479.kotlinmcui.scope.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.scope.childrenMaxWidth

context(ctx: DslContext)
fun Box(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
) = collect(object : DslScope by DslScopeImpl(newChildId(id ?: function::class), modifier, ctx, function) {
    val lazyWidth by lazy { childrenMaxWidth }

    context(instance: DslComponent)
    override val contentMinWidth get() = Measure.max(lazyWidth, super.contentMinWidth)

    val lazyHeight by lazy { childrenMaxHeight }

    context(instance: DslComponent)
    override val contentMinHeight get() = Measure.max(lazyHeight, super.contentMinHeight)
})


context(ctx: DslContext)
fun DslChild.Box(function: DslFunction) {
    val component = currentComponent()
    remove(this)
    Box(component.modifier,id = function::class) {
        function()
        collect(component)
    }
}