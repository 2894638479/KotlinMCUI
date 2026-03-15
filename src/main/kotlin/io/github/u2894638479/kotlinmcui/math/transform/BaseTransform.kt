package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import kotlin.math.cos
import kotlin.math.sin

sealed interface BaseTransform: Transform {
    override val baseTransforms get() = listOf(this)
}

class Rotate(val rad: Double): BaseTransform {
    override fun transform(pos: Position): Position {
        val cos = cos(rad)
        val sin = sin(rad)
        val x = pos.x * cos - pos.y * sin
        val y = pos.x * sin + pos.y * cos
        return Position(x, y)
    }

    override fun inverse(pos: Position) = Rotate(-rad).transform(pos)
}

class Translate(val x: Measure, val y: Measure): BaseTransform {
    constructor(pos: Position): this(pos.x,pos.y)
    inline val pos get() = Position(x,y)
    override fun transform(pos: Position) = Position(pos.x + x,pos.y + y)
    override fun inverse(pos: Position) = Position(pos.x - x,pos.y - y)
}

class Scale(val x: Double, val y: Double): BaseTransform {
    override fun transform(pos: Position) = Position(pos.x * x,pos.y * y)
    override fun inverse(pos: Position) = Position(pos.x / x,pos.y / y)
}
