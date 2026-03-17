package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Position

interface Transform {
    fun transform(pos: Position): Position
    fun inverse(pos: Position): Position
    val baseTransforms: List<BaseTransform>
    companion object {
        val empty = object :Transform {
            override fun transform(pos: Position) = pos
            override fun inverse(pos: Position) = pos
            override val baseTransforms get() = emptyList<BaseTransform>()
        }
        val Transform.isEmpty get() = baseTransforms.isEmpty()
        operator fun Transform.plus(other: Transform) = Transforms(baseTransforms + other.baseTransforms)
    }
}