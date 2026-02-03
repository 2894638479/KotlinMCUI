package io.github.u2894638479.kotlinmcui.glfw

enum class MouseButton{
    LEFT,RIGHT,MIDDLE,BUTTON4,BUTTON5,BUTTON6,BUTTON7,LAST;
    companion object {
        fun from(value:Int) = entries.getOrNull(value) ?: error("unknown GLFW Mouse Button $value")
    }
}