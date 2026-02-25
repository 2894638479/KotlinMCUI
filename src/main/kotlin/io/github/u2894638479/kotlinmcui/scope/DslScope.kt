package io.github.u2894638479.kotlinmcui.scope

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position

interface DslScope : DslComponent {

    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() =
        instance.children.asReversed().forEach { it.run { render() } }

    context(eventModifier: EventModifier)
    override fun keyDown(key: Int, scanCode: Int) =
        instance.children.firstOrNull { it.run { keyDown(key, scanCode) } } != null

    context(eventModifier: EventModifier)
    override fun keyUp(key: Int, scanCode: Int) =
        instance.children.firstOrNull { it.run { keyUp(key, scanCode) } } != null

    context(eventModifier: EventModifier, mouse: Position)
    override fun mouseDown(mouseButton: MouseButton) =
        instance.children.firstOrNull { it.run { mouseDown(mouseButton) } } != null

    context(eventModifier: EventModifier, mouse: Position)
    override fun mouseUp(mouseButton: MouseButton) =
        instance.children.firstOrNull { it.run { mouseUp(mouseButton) } } != null

    context(mouse: Position)
    override fun mouseMove() =
        instance.children.forEach { it.run { mouseMove() } }

    context(mouse: Position)
    override fun mouseScrollVertical(amount: Double): Double {
        var remain = amount
        instance.children.forEach {
            if(remain == 0.0) return 0.0
            remain = it.run { mouseScrollVertical(remain) }
        }
        return remain
    }

    context(mouse: Position)
    override fun mouseScrollHorizontal(amount: Double): Double {
        var remain = amount
        instance.children.forEach {
            if(remain == 0.0) return 0.0
            remain = it.run { mouseScrollHorizontal(remain) }
        }
        return remain
    }

    context(eventModifier: EventModifier)
    override fun charTyped(c: Char) =
        instance.children.firstOrNull { it.run { charTyped(c) } } != null

    override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? =
        instance.children.firstNotNullOfOrNull { it.testHit(mouse, get) } ?: super.testHit(mouse, get)

    override fun <T> testHit(get: context(DslComponent) (DslComponent) -> T?): T? =
        instance.children.firstNotNullOfOrNull { it.testHit(get) } ?: super.testHit(get)

    override fun focusChanged(newFocus: DslId?) = instance.children.forEach { it.run { focusChanged(newFocus) } }

    override fun hoverChanged(newHover: DslId?) = instance.children.forEach { it.run { hoverChanged(newHover) } }

    override val viewHorizontal: List<List<DslComponent>> get() = instance.children.groupBy { it.rect.run { left + right } }.toSortedMap().values.toList()
    override val viewVertical: List<List<DslComponent>> get() = instance.children.groupBy { it.rect.run { top + bottom } }.toSortedMap().values.toList()
    override val viewSequential: List<DslComponent> get() = instance.children

    override val narration get() = instance.children.mapNotNull { it.run { narration } }.joinToString()
}