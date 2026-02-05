package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun ColorRect(modifier: Modifier = Modifier, color: Color, id:Any) = collect(
    object : DslComponent by DslComponentImpl(newChildId(id), modifier) {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
        override fun <RP> render(mouse: Position) {
            backend.fillRect(instance.rect, color)
        }
    }
)