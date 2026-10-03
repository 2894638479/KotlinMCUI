package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.mixed
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.dataStore
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.dsl.withColor
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

context(ctx: DslContext)
fun Image(
    modifier: Modifier = Modifier,
    image: ImageHolder,
    strategy: ImageStrategy = ImageStrategy.clip,
    id:Any
) = collect(object : DslComponent by DslComponentImpl(newChildId(id), modifier,ctx) {
    val color = Color.WHITE.mixed
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        strategy.render(instance.rect, image, color)
        backend.flush()
        if (instance.isHighlighted) {
            backend.fillRect(instance.rect, Color.WHITE.change(a = 50))
            backend.flush()
        }
    }
})

/**
 * A repeated background image, tinted to a quarter of the current theme
 * colour so it reads as a backdrop rather than foreground content.
 */
context(ctx: DslContext)
fun BackgroundImage(image: ImageHolder) = withColor(Color(0.25, 0.25, 0.25).mixed) {
    Image(
        image = image,
        strategy = ImageStrategy.repeat(scale = dataStore.scale),
        id = image
    )
}

