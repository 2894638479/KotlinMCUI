package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.sumOf

interface DslScope : DslComponent {
    override val children: DslChild.List

    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) =
        instance.children?.asReversed()?.forEach { it.run { render(mouse) } } ?: Unit

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun keyDown(key: Int, scanCode: Int) =
        instance.children?.firstOrNull { it.run { keyDown(key, scanCode) } } != null

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun keyUp(key: Int, scanCode: Int) =
        instance.children?.firstOrNull { it.run { keyUp(key, scanCode) } } != null

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun mouseDown(mouse: Position, mouseButton: MouseButton) =
        instance.children?.firstOrNull { it.run { mouseDown(mouse, mouseButton) } } != null

    context(instance: DslComponent, eventModifier: EventModifier)
    override fun mouseUp(mouse: Position, mouseButton: MouseButton) =
        instance.children?.firstOrNull { it.run { mouseUp(mouse, mouseButton) } } != null

    context(instance: DslComponent)
    override fun mouseMove(mouse: Position) =
        instance.children?.forEach { it.run { mouseMove(mouse) } } ?: Unit

    context(instance: DslComponent)
    override fun mouseScrollVertical(mouse: Position, amount: Double): Double {
        var remain = amount
        instance.children?.forEach {
            if(remain == 0.0) return 0.0
            remain = it.run { mouseScrollVertical(mouse, remain) }
        }
        return remain
    }

    context(instance: DslComponent)
    override fun mouseScrollHorizontal(mouse: Position, amount: Double): Double {
        var remain = amount
        instance.children?.forEach {
            if(remain == 0.0) return 0.0
            remain = it.run { mouseScrollHorizontal(mouse, remain) }
        }
        return remain
    }

    context(instance: DslComponent)
    override fun charTyped(c: Char, eventModifier: EventModifier) =
        instance.children?.firstOrNull { it.run { charTyped(c, eventModifier) } } != null

    context(instance: DslComponent)
    override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? =
        instance.children?.firstNotNullOfOrNull { it.run { testHit(mouse, get) } } ?: super.testHit(mouse, get)

    context(instance: DslComponent)
    override fun <T> testHit(get: context(DslComponent) (DslComponent) -> T?): T? =
        instance.children?.firstNotNullOfOrNull { it.run { testHit(get) } } ?: super.testHit(get)

    context(instance: DslComponent)
    override fun focusChanged(newFocus: DslId?) = instance.children?.forEach { it.run { focusChanged(newFocus) } } ?: Unit

    context(instance: DslComponent)
    override fun hoverChanged(newHover: DslId?) = instance.children?.forEach { it.run { hoverChanged(newHover) } } ?: Unit

    override val viewHorizontal: List<List<DslComponent>> get() = children.groupBy { it.rect.run { left + right } }.toSortedMap().values.toList()
    override val viewVertical: List<List<DslComponent>> get() = children.groupBy { it.rect.run { top + bottom } }.toSortedMap().values.toList()
    override val viewSequential: List<DslComponent> get() = children

    context(instance: DslComponent)
    override val narration get() = instance.children?.mapNotNull { it.run { narration } }?.joinToString()
}