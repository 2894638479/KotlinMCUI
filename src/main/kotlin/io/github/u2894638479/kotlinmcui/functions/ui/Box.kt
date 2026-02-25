package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.remove
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslScope
import io.github.u2894638479.kotlinmcui.scope.DslScopeImpl

context(ctx: DslContext)
fun Box(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
) = collect(object : DslScope by DslScopeImpl(newChildId(id ?: function::class), modifier, ctx, function) {
    override val contentMinWidth by lazy {
        max(instance.childrenMaxWidth,super.contentMinWidth)
    }

    override val contentMinHeight by lazy {
        max(instance.childrenMaxHeight,super.contentMinHeight)
    }
})


context(ctx: DslContext)
fun DslChild.Box(id:Any? = null,function: DslFunction) {
    val component = currentComponent()
    remove(this)
    Box(component.modifier,id = id ?: function::class) {
        function()
        collect(component)
    }
}