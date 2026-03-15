package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Position

interface Transform {
    fun transform(pos: Position): Position
    fun inverse(pos: Position): Position
    val baseTransforms: List<BaseTransform>
}