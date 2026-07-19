package io.github.u2894638479.kotlinmcui.input

/**
 * Mouse button identifiers.
 *
 * The mapping to physical buttons follows the convention shared by GLFW and SDL:
 * [LEFT]=0, [RIGHT]=1, [MIDDLE]=2, then additional buttons 4–7. [LAST] is a
 * sentinel value equal to the highest valid index.
 */
enum class MouseButton {
    LEFT, RIGHT, MIDDLE, BUTTON4, BUTTON5, BUTTON6, BUTTON7, LAST;

    companion object {
        /** Look up a [MouseButton] by its index. Throws for out-of-range values. */
        fun from(value: Int) = entries.getOrNull(value) ?: error("unknown mouse button index $value")
    }
}
