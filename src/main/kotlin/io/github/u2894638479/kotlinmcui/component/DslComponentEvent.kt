package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Position
import java.nio.file.Path

/**
 * Input event handling contract for a DSL component.
 *
 * Events propagate **parent-to-child**: each component dispatches to its
 * children in order until one consumes the event (returns `true`). This is
 * the standard bubbling model for keyboard, mouse, scroll, and character input.
 *
 * Contrast with [DslComponentGlobalEvent] which is dispatched from the root
 * and may target specific components.
 *
 * Scroll methods return the unconsumed scroll amount rather than a boolean.
 */
interface DslComponentEvent {
    /**
     * Render this component.
     * @param RP The platform-specific render parameter type.
     */
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    fun <RP> render() {}

    /** A key was pressed. */
    context(eventModifier: KeyModifiers)
    fun keyDown(key: Key, scanCode: Int) = false

    /** A key was released. */
    context(eventModifier: KeyModifiers)
    fun keyUp(key: Key, scanCode: Int) = false

    /** A mouse button was pressed. */
    context(eventModifier: KeyModifiers, mouse: Position)
    fun mouseDown(mouseButton: MouseButton) = false

    /** A mouse button was released. */
    context(eventModifier: KeyModifiers, mouse: Position)
    fun mouseUp(mouseButton: MouseButton) = false

    /** The mouse moved. */
    context(mouse: Position)
    fun mouseMove() {}

    /**
     * The mouse wheel was scrolled vertically.
     * @param amount The scroll delta.
     * @return The unconsumed scroll amount (pass-through if not handled).
     */
    context(mouse: Position)
    fun mouseScrollVertical(amount: Double) = amount

    /**
     * The mouse wheel was scrolled horizontally.
     * @return The unconsumed scroll amount.
     */
    context(mouse: Position)
    fun mouseScrollHorizontal(amount: Double) = amount

    /**
     * A character was typed (for text input fields).
     * Unlike key events, character input does not carry modifier state —
     * composed characters from IME are delivered directly.
     */
    fun charTyped(c: Char) = false

    /**
     * Hit-test: determine which component is at [mouse].
     * @param get A selector invoked on the target component.
     * @return The selected value, or `null` if this component does not match.
     */
    fun <T> testHit(mouse: Position, get: (DslComponent) -> T?): T? = null

    /**
     * Hit-test without a position constraint.
     * @see testHit
     */
    fun <T> testHit(get: (DslComponent) -> T?): T? = null

    /** Files were dropped onto this component. Returns `true` if handled. */
    context(mouse: Position)
    fun dropFiles(files: Array<Path>) = false
}
