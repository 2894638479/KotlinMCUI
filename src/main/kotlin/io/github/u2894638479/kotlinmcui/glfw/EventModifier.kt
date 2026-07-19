package io.github.u2894638479.kotlinmcui.glfw

import org.lwjgl.glfw.GLFW

/**
 * Wraps a GLFW modifier key bitmask with readable boolean properties.
 *
 * Each property returns `true` if the corresponding modifier key is held
 * during the current event.
 */
@JvmInline
value class EventModifier(val value:Int) {
    private fun has(mod:Int) = (value and mod) != 0
    val shift get() = has(GLFW.GLFW_MOD_SHIFT)
    val ctrl get() = has(GLFW.GLFW_MOD_CONTROL)
    val alt get() = has(GLFW.GLFW_MOD_ALT)
    val `super` get() = has(GLFW.GLFW_MOD_SUPER)
    val capsLock get() = has(GLFW.GLFW_MOD_CAPS_LOCK)
    val numLock get() = has(GLFW.GLFW_MOD_NUM_LOCK)
}