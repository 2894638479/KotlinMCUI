package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.scope.DslChild

context(ctx: DslContext)
fun DefaultBackground(id:Any = object{}::class) = Spacer(id = id).defaultBackground()

context(ctx: DslContext)
fun DslChild.defaultBackground() = change { object:DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.renderDefaultBackground(instance.rect)
        it.render(mouse)
    }
} }