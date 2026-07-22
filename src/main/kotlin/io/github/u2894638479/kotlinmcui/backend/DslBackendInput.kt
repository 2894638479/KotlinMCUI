package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.Rect

/**
 * Input state queries provided by the backend.
 *
 * Key codes use the framework's platform-neutral [Key] type. The backend is
 * responsible for translating its platform-specific key codes to [Key] before
 * dispatching events. Mouse button indices follow the convention shared by
 * GLFW and SDL (0=left, 1=right, 2=middle, then additional buttons).
 */
interface DslBackendInput {
    /** Returns `true` while the given [key] is held down. */
    fun isKeyDown(key: Key): Boolean

    /** Returns `true` while the given mouse button index is held down. */
    fun isMouseDown(mouse: Int): Boolean

    /** The current mouse cursor position in logical pixels. */
    val mouse: Position

    /**
     * Notify the platform of the rectangle where the IME candidate window
     * should appear. Called each frame by the framework when a focused
     * component declares a non-null [io.github.u2894638479.kotlinmcui.component.DslComponentMetadata.imeArea].
     */
    fun setImeArea(rect: Rect)
}
