package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun Image(
    modifier: Modifier = Modifier,
    image: ImageHolder,
    color: Color = Color.WHITE,
    strategy: ImageStrategy = ImageStrategy.clip,
    id:Any
) = collect(object : DslComponent by DslComponentImpl(newChildId(id), modifier) {
    context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        strategy.render(instance.rect, image, color)
        if (isHighlighted) backend.fillRect(instance.rect, Color.WHITE.change(a = 50))
    }
})

context(ctx: DslContext)
fun BackgroundImage(image: ImageHolder) = Image(
    image = image,
    strategy = ImageStrategy.repeat(scale = dataStore.scale),
    color = Color(0.25, 0.25, 0.25),
    id = image
)

