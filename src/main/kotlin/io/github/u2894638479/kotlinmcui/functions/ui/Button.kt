package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun Button(
    modifier: Modifier = Modifier,
    color: Color = Color.WHITE,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).change { object : DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.renderButton(instance.rect, isHighlighted,instance.highlightable,color)
        it.render(mouse)
    }
}}