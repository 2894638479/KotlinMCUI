package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Position

interface DslComponentEvent {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    fun <RP> render(mouse: Position) {}

    context(instance: DslComponent)
    fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier) = false

    context(instance: DslComponent)
    fun keyUp(key: Int, scanCode: Int, eventModifier: EventModifier) = false

    context(instance: DslComponent)
    fun mouseDown(mouse: Position, mouseButton: MouseButton) = false

    context(instance: DslComponent)
    fun mouseUp(mouse: Position, mouseButton: MouseButton) = false

    context(instance: DslComponent)
    fun mouseMove(mouse: Position) {}

    context(instance: DslComponent)
    fun mouseScroll(mouse: Position, amount: Double) = amount

    context(instance: DslComponent)
    fun charTyped(c: Char, eventModifier: EventModifier) = false

    context(instance: DslComponent)
    fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?): T? = null

    context(instance: DslComponent)
    fun <T> testHit(get: context(DslComponent) (DslComponent) -> T?): T? = null

    context(instance: DslComponent)
    fun focusChanged(newFocus: DslId?) {}

    context(instance: DslComponent)
    fun onHover(hovered: Boolean) {}

    context(instance: DslComponent)
    fun onScrolledInScrollable(percentage: Double) {}
}