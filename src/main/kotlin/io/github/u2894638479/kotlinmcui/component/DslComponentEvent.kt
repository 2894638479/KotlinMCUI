package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Position
import java.nio.file.Path

interface DslComponentEvent {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    fun <RP> render() {}

    context(eventModifier: EventModifier)
    fun keyDown(key: Int, scanCode: Int) = false

    context(eventModifier: EventModifier)
    fun keyUp(key: Int, scanCode: Int) = false

    context(eventModifier: EventModifier, mouse: Position)
    fun mouseDown(mouseButton: MouseButton) = false

    context(eventModifier: EventModifier, mouse: Position)
    fun mouseUp(mouseButton: MouseButton) = false

    context(mouse: Position)
    fun mouseMove() {}

    context(mouse: Position)
    fun mouseScrollVertical(amount: Double) = amount

    context(mouse: Position)
    fun mouseScrollHorizontal(amount: Double) = amount

    context(eventModifier: EventModifier)
    fun charTyped(c: Char) = false

    fun <T> testHit(mouse: Position, get: (DslComponent) -> T?): T? = null

    fun <T> testHit(get: (DslComponent) -> T?): T? = null

    context(mouse: Position)
    fun dropFiles(files: Array<Path>) = false
}