package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.math.Position

interface DslBackendInput {
    fun isKeyDown(key: Int): Boolean
    fun isMouseDown(mouse: Int): Boolean
    val mouse: Position
}