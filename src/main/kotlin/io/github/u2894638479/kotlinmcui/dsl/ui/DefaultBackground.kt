package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.container.DslChild

context(ctx: DslContext)
fun DefaultBackground(id:Any = object{}::class) = Spacer(id = id).defaultBackground()

context(ctx: DslContext)
fun DslChild.defaultBackground() = change { object:DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderDefaultBackground(instance.rect)
        it.render()
    }
} }