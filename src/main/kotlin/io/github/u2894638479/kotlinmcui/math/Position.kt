package io.github.u2894638479.kotlinmcui.math
import io.github.u2894638479.kotlinmcui.math.animate.Interpolatable
import kotlinx.serialization.Serializable

@Serializable
class Position (
    val x: Measure,
    val y: Measure
): Interpolatable<Position> {
    override operator fun plus(other: Position) = Position(x + other.x,y + other.y)
    override operator fun minus(other: Position) = Position(x - other.x,y - other.y)

    override operator fun times(other: Double) = Position(x * other,y * other)
    operator fun times(value: Float) = Position(x * value,y * value)
    operator fun times(value: Int) = Position(x * value,y * value)
    override operator fun div(other: Double) = Position(x / other,y / other)
    operator fun div(value: Float) = Position(x / value,y / value)
    operator fun div(value: Int) = Position(x / value,y / value)

    operator fun unaryMinus() = Position(-x,-y)

    fun pos(axis: Axis) = when(axis) {
        Axis.Horizontal -> x
        Axis.Vertical -> y
    }
}