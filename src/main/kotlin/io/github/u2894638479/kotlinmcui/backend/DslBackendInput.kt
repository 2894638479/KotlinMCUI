package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.math.Position

/**
 * Input state queries provided by the backend.
 *
 * All key and mouse button codes follow GLFW constants (e.g. `GLFW.GLFW_KEY_ENTER`).
 */
interface DslBackendInput {
    /** Returns `true` while the given GLFW key code is held down. */
    fun isKeyDown(key: Int): Boolean

    /** Returns `true` while the given GLFW mouse button is held down. */
    fun isMouseDown(mouse: Int): Boolean

    /** The current mouse cursor position in logical pixels. */
    val mouse: Position
}
