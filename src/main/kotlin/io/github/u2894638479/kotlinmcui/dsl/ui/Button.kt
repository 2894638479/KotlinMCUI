package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.mixed
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.container.DslChild

/** Decorator that renders a Minecraft-style button border around the component. */
context(ctx: DslContext)
fun DslChild.buttonBackground(padding: Measure = 2.scaled) = change {
    val color = Color.WHITE.mixed
    object:DslComponent by it {
        context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.renderButton(instance.rect.expand(padding), instance.isHighlighted,instance.highlightable,color)
            it.render()
        }

        override val narratable get() = true
        override val narration get() = "${it.run { narration ?: "" }} ${translate("kotlinmcui.narration.button")}"
        override val modifier = it.modifier.padding(padding)
    }
}

/**
 * Create a button with a Minecraft-style background texture, tinted by the
 * current theme colour scope ([withColor]/`Color.mixed`).
 *
 * **Note:** This only provides the visual button border. To make the button
 * interactive, apply [clickable]:
 * ```
 * Button { Text { "Click Me" } }.clickable { ... }
 * ```
 *
 * @param modifier The modifier for sizing and padding.
 * @param id Unique identifier.
 * @param function DSL builder for the button's content.
 */
context(ctx: DslContext)
fun Button(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
) = Box(modifier,id,function).buttonBackground(0.px)