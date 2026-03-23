package io.github.u2894638479.kotlinmcui.functions.decorator

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import io.github.u2894638479.kotlinmcui.functions.autoAnimate
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
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
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.scope.DslChild.Companion.reverseRead
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


context(ctx: DslContext)
fun DslChild.mask(color: Color) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        it.render()
        backend.fillRect(instance.rect,color)
    }
}}

context(ctx: DslContext)
fun DslChild.background(color: Color) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.fillRect(instance.rect,color)
        it.render()
    }
} }

context(ctx: DslContext)
fun DslChild.backgroundImage(
    image: ImageHolder,
    color: Color = Color.WHITE,
    strategy: ImageStrategy = ImageStrategy.clip,
) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        strategy.render(instance.rect,image,color)
        it.render()
    }
}}

context(backend: DslBackendRenderer<RP>, renderParam: RP)
private fun <RP> renderOutline(rect: Rect, widthIn: Measure, widthOut: Measure, color: Color) {
    backend.fillRect(Rect(rect.left - widthOut, rect.top - widthOut, rect.left + widthIn, rect.bottom + widthOut),color)
    backend.fillRect(Rect(rect.right - widthIn, rect.top - widthOut, rect.right + widthOut, rect.bottom + widthOut),color)
    backend.fillRect(Rect(rect.left + widthIn, rect.top - widthOut, rect.right - widthIn, rect.top + widthIn),color)
    backend.fillRect(Rect(rect.left + widthIn, rect.bottom - widthIn, rect.right - widthIn, rect.bottom + widthOut),color)
}

context(ctx: DslContext)
fun DslChild.highlightBox(widthIn: Measure = 0.px, widthOut: Measure = 1.scaled, color: Color = Color.WHITE) = change { object : DslComponent by it {
    override val highlightable get() = true

    override val modifier get() = it.modifier.padding(widthOut)

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        if(instance.isHighlighted) renderOutline(instance.rect, widthIn, widthOut, color)
        it.render()
    }
}}

context(ctx: DslContext)
fun DslChild.outline(widthIn: Measure = 0.px, widthOut: Measure = 1.scaled, color: Color = Color.WHITE) = change { object : DslComponent by it {
    override val modifier get() = it.modifier.padding(widthOut)

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        renderOutline(instance.rect, widthIn, widthOut, color)
        it.render()
    }
}}

context(ctx: DslContext)
fun DslChild.hoverMask(highlightColor: Color = Color(255, 255, 255, 80)) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        it.render()
        if(mouse in instance.rect) backend.fillRect(instance.rect,highlightColor)
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
        val value by autoAnimate(it.contentMinHeight.unscaled, duration, interpolator)
        return value.scaled
    }
}}

context(ctx: DslContext)
fun DslChild.animateWidth(duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default)
= change { object: DslComponent by it {
    override val contentMinWidth: Measure get() {
        val value by autoAnimate(it.contentMinWidth.unscaled, duration, interpolator)
        return value.scaled
    }
}}

context(ctx: DslContext)
fun DslChild.containerBackground(padding: Measure = 3.scaled)
= change { object : DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderContainer(rect.expand(padding))
        it.render()
    }

    override val modifier get() = it.modifier.padding(padding)
} }

context(ctx: DslContext)
fun DslChild.slotBackground(padding: Measure = 1.scaled)
= change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderSlot(rect.expand(padding))
        it.render()
    }

    override val modifier get() = it.modifier.padding(padding)
} }

context(ctx: DslContext)
fun DslChild.renderScissor() = change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() = backend.withScissor(instance.rect) {
            it.render()
        }
    }
}

context(ctx: DslContext)
fun DslChild.tooltipBackground(padding: Measure = 3.px) = change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.renderTooltip(instance.rect.expand(padding))
            it.render()
        }

        override val modifier = it.modifier.padding(3.px)
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
        context(eventModifier: EventModifier, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton) = context(getTransform(it).invert() * mouse) { it.mouseDown(mouseButton) }
        context(mouse: Position)
        override fun mouseMove() = context(getTransform(it).invert() * mouse) { it.mouseMove() }
        context(eventModifier: EventModifier, mouse: Position)
        override fun mouseUp(mouseButton: MouseButton) = context(getTransform(it).invert() * mouse) { it.mouseUp(mouseButton) }
        override fun <T> testHit(mouse: Position, get: (DslComponent) -> T?) = it.testHit(getTransform(it).invert() * mouse,get)
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() { backend.withTransform(getTransform(it)) { it.render() } }
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