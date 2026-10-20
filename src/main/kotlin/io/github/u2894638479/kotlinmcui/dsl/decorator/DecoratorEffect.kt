/**
 * Visual decorator functions for [DslChild] components.
 *
 * Each function wraps a component using the [DslChild.change] pattern
 * (decorator via delegation) to add visual effects: backgrounds, outlines,
 * transforms (rotate/scale/move), animations, and masks.
 */
package io.github.u2894638479.kotlinmcui.dsl.decorator

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.backend.fillRect
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.mixed
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.center
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.math.transform.Transform
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslChild.Companion.reverseRead
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.nio.file.Path


context(ctx: DslContext)
fun DslChild.mask(color: Color) = change { object: DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        it.render()
        backend.fillRect(instance.rect,color)
    }
}}

context(ctx: DslContext)
fun DslChild.background(color: Color? = null) = change { val c = color ?: Color.WHITE.mixed; object: DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.fillRect(instance.rect,c)
        it.render()
    }
} }

context(ctx: DslContext)
fun DslChild.backgroundImage(
    image: ImageHolder,
    color: Color? = null,
    strategy: ImageStrategy = ImageStrategy.clip,
) = change { val c = color ?: Color.WHITE.mixed; object: DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        strategy.render(instance.rect,image,c)
        it.render()
    }
}}

context(backend: DslRootRenderer<RP>, renderParam: RP)
private fun <RP> renderOutline(rect: Rect, widthIn: Measure, widthOut: Measure, color: Color) {
    backend.fillRect(Rect(rect.left - widthOut, rect.top - widthOut, rect.left + widthIn, rect.bottom + widthOut),color)
    backend.fillRect(Rect(rect.right - widthIn, rect.top - widthOut, rect.right + widthOut, rect.bottom + widthOut),color)
    backend.fillRect(Rect(rect.left + widthIn, rect.top - widthOut, rect.right - widthIn, rect.top + widthIn),color)
    backend.fillRect(Rect(rect.left + widthIn, rect.bottom - widthIn, rect.right - widthIn, rect.bottom + widthOut),color)
}

context(ctx: DslContext)
fun DslChild.highlightBox(widthIn: Measure = 0.px, widthOut: Measure = 1.scaled, color: Color? = null) = change { val c = color ?: Color.WHITE.mixed; object : DslComponent by it {
    override val highlightable get() = true

    override val modifier get() = it.modifier.padding(widthOut)

    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        if(instance.isHighlighted) renderOutline(instance.rect, widthIn, widthOut, c)
        it.render()
    }
}}

context(ctx: DslContext)
fun DslChild.outline(widthIn: Measure = 0.px, widthOut: Measure = 1.scaled, color: Color? = null) = change { val c = color ?: Color.WHITE.mixed; object : DslComponent by it {
    override val modifier get() = it.modifier.padding(widthOut)

    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        renderOutline(instance.rect, widthIn, widthOut, c)
        it.render()
    }
}}

context(ctx: DslContext)
fun DslChild.hoverMask(highlightColor: Color = Color(255, 255, 255, 80)) = change { object: DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        it.render()
        if(mouse in instance.rect) {
            backend.fillRect(instance.rect,highlightColor)
        }
    }
}}

context(ctx: DslContext)
fun DslChild.shrink() = change { object: DslComponent by it {
    override val contentMinWidth get() = 0.px
    override val contentMinHeight get() = 0.px
}}

context(ctx: DslContext)
fun DslChild.widthRate(rate: Double) = change { object: DslComponent by it {
    override val contentMinWidth get() = it.contentMinWidth * rate
}}

context(ctx: DslContext)
fun DslChild.heightRate(rate: Double) = change { object: DslComponent by it {
    override val contentMinHeight get() = it.contentMinHeight * rate
}}

context(ctx: DslContext)
fun DslChild.animateHeight(duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default)
= change { object: DslComponent by it {
    override val contentMinHeight: Measure get() {
        val unscaled = it.contentMinHeight.unscaled
        val value by local.autoAnimate(duration = duration, interpolator = interpolator) { unscaled }
        return value.scaled
    }
}}

context(ctx: DslContext)
fun DslChild.animateWidth(duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default)
= change { object: DslComponent by it {
    override val contentMinWidth: Measure get() {
        val unscaled = it.contentMinWidth.unscaled
        val value by local.autoAnimate(duration = duration, interpolator = interpolator) { unscaled }
        return value.scaled
    }
}}

context(ctx: DslContext)
fun DslChild.containerBackground(padding: Measure = 3.scaled)
= change { object : DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderContainer(rect.expand(padding))
        it.render()
    }

    override val modifier get() = it.modifier.padding(padding)
} }

context(ctx: DslContext)
fun DslChild.slotBackground(padding: Measure = 1.scaled)
= change { object: DslComponent by it {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderSlot(rect.expand(padding))
        it.render()
    }

    override val modifier get() = it.modifier.padding(padding)
} }

context(ctx: DslContext)
fun DslChild.renderScissor() = change {
    object : DslComponent by it {
        context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() = backend.withScissor(instance.rect) {
            it.render()
        }
    }
}

context(ctx: DslContext)
fun DslChild.tooltipBackground(padding: Measure = 3.px) = change {
    object : DslComponent by it {
        context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.renderTooltip(instance.rect.expand(padding))
            it.render()
        }

        override val modifier get() = it.modifier.padding(padding)
    }
}

context(ctx: DslContext)
fun DslChild.reverseChildren() = change {
    object: DslComponent by it {
        override val children get() = it.children.reverseRead()
    }
}

context(ctx: DslContext)
private fun DslChild.transform(getTransform:DslComponent.(DslComponent) -> Transform) = change {
    object : DslComponent by it {
        override val transform by lazy { it.transform * getTransform(it) }

        // Every entry point that carries a mouse position must convert it into the
        // subtree's own coordinate space, otherwise hit testing and rendering would
        // disagree about where the component is.
        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton) = context(getTransform(it).invert() * mouse) { it.mouseDown(mouseButton) }
        context(mouse: Position)
        override fun mouseMove() = context(getTransform(it).invert() * mouse) { it.mouseMove() }
        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseUp(mouseButton: MouseButton) = context(getTransform(it).invert() * mouse) { it.mouseUp(mouseButton) }
        context(mouse: Position)
        override fun mouseScrollVertical(amount: Double) = context(getTransform(it).invert() * mouse) { it.mouseScrollVertical(amount) }
        context(mouse: Position)
        override fun mouseScrollHorizontal(amount: Double) = context(getTransform(it).invert() * mouse) { it.mouseScrollHorizontal(amount) }
        context(mouse: Position)
        override fun dropFiles(files: Array<Path>) = context(getTransform(it).invert() * mouse) { it.dropFiles(files) }
        override fun <T> testHit(mouse: Position, get: (DslComponent) -> T?) = it.testHit(getTransform(it).invert() * mouse,get)
        context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val t = getTransform(it)
            backend.withTransform(t) {
                context(t.invert() * mouse) { it.render() }
            }
        }
    }
}

context(ctx: DslContext)
fun DslChild.rotate(rad: Double) = if(rad == 0.0) this else transform {
    val center = it.transform * instance.rect.center
    Transform.rotate(center,rad)
}

context(ctx: DslContext)
fun DslChild.scale(x:Double,y:Double = x) = if(x == 1.0 && y == 1.0) this else transform {
    val center = it.transform * instance.rect.center
    Transform.scale(center,x,y)
}

context(ctx: DslContext)
fun DslChild.move(x: Measure,y: Measure) = if(x == 0.px && y == 0.px) this else transform { Transform.translate(x,y) }

context(ctx: DslContext)
fun DslChild.move(pos: Position) = move(pos.x,pos.y)

context(ctx: DslContext)
fun DslChild.shear(shx: Double, shy: Double) = if (shx == 0.0 && shy == 0.0) this else transform {
    val center = it.transform * instance.rect.center
    Transform.shear(center, shx, shy)
}

context(ctx: DslContext)
fun DslChild.perspective(px: Double, py: Double) = if (px == 0.0 && py == 0.0) this else transform {
    val center = it.transform * instance.rect.center
    Transform.perspective(center, px, py)
}