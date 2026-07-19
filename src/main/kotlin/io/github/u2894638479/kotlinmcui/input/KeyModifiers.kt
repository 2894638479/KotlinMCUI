package io.github.u2894638479.kotlinmcui.input

/**
 * Keyboard modifier state at the time of an input event.
 *
 * The backend populates this from platform-specific modifier bitmasks
 * (e.g. GLFW `GLFW_MOD_SHIFT`, SDL3 `SDL_KMOD_SHIFT`) before dispatching
 * events to the DSL framework.
 */
data class KeyModifiers(
    val shift: Boolean = false,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val superKey: Boolean = false,
    val capsLock: Boolean = false,
    val numLock: Boolean = false
)
