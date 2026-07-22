package io.github.u2894638479.kotlinmcui.input

/**
 * Mouse button identifiers following the convention shared by GLFW and SDL:
 * [LEFT]=0, [RIGHT]=1, [MIDDLE]=2, then additional buttons 4–7.
 */
enum class MouseButton {
    LEFT, RIGHT, MIDDLE, BUTTON4, BUTTON5, BUTTON6, BUTTON7;

    companion object {
        /** Look up a [MouseButton] by its index. Throws for out-of-range values. */
        fun from(value: Int) = entries.getOrNull(value) ?: error("unknown mouse button index $value")
    }
}
