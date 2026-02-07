package io.github.u2894638479.kotlinmcui.functions.decorator

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.autoAnimate
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.*
import io.github.u2894638479.kotlinmcui.math.animate.Interpolator
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.scope.DslChild
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


context(ctx: DslContext)
fun DslChild.mask(color: Color) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        it.render(mouse)
        backend.fillRect(instance.rect,color)
    }
}}

context(ctx: DslContext)
fun DslChild.background(color: Color) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.fillRect(instance.rect,color)
        it.render(mouse)
    }
} }

context(ctx: DslContext)
fun DslChild.backgroundImage(
    image: ImageHolder,
    color: Color = Color.WHITE,
    strategy: ImageStrategy = ImageStrategy.clip,
) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        strategy.render(instance.rect,image,color)
        it.render(mouse)
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
fun DslChild.highlightBox(widthIn: Measure = 0.5.scaled, widthOut: Measure = 0.5.scaled, color: Color = Color.WHITE) = change { object : DslComponent by it {
    context(instance: DslComponent)
    override val highlightable get() = true

    override val modifier get() = it.modifier.padding(widthOut)

    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        if(isHighlighted) renderOutline(instance.rect, widthIn, widthOut, color)
        it.render(mouse)
    }
}}

context(ctx: DslContext)
fun DslChild.outline(widthIn: Measure = 0.5.scaled, widthOut: Measure = 0.5.scaled, color: Color = Color.WHITE) = change { object : DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        renderOutline(instance.rect, widthIn, widthOut, color)
        it.render(mouse)
    }
}}

context(ctx: DslContext)
fun DslChild.hoverMask(highlightColor: Color = Color(255, 255, 255, 80)) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        it.render(mouse)
        if(dataStore.hovered == instance.identity) backend.fillRect(instance.rect,highlightColor)
    }
}}

context(ctx: DslContext)
fun DslChild.shrink() = change { object: DslComponent by it {
    context(instance: DslComponent)
    override val contentMinWidth get() = 0.px
    context(instance: DslComponent)
    override val contentMinHeight get() = 0.px
}}

context(ctx: DslContext)
fun DslChild.animateHeight(duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default)
= change { object: DslComponent by it {
    context(instance: DslComponent)
    override val contentMinHeight: Measure get() {
        val value by autoAnimate(it.contentMinHeight, duration, interpolator)
        return value
    }
}}

context(ctx: DslContext)
fun DslChild.animateWidth(duration: Duration = 0.5.seconds, interpolator: Interpolator = Interpolator.default)
= change { object: DslComponent by it {
    context(instance: DslComponent)
    override val contentMinWidth: Measure get() {
        val value by autoAnimate(it.contentMinWidth, duration, interpolator)
        return value
    }
}}

context(ctx: DslContext)
fun DslChild.containerBackground(padding: Measure = 3.scaled)
= change { object : DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.renderContainer(rect.expand(padding))
        it.render(mouse)
    }

    override val modifier get() = it.modifier.padding(padding)
} }

context(ctx: DslContext)
fun DslChild.slotBackground(padding: Measure = 0.px)
= change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.renderSlot(rect.expand(padding))
        it.render(mouse)
    }

    override val modifier get() = it.modifier.padding(padding)
} }