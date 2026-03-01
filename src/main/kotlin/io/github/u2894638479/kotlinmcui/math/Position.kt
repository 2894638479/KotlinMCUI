package io.github.u2894638479.kotlinmcui.math
import kotlinx.serialization.Serializable

@Serializable
class Position (
    val x: Measure,
    val y: Measure
) {
    operator fun plus(other: Position) = Position(x + other.x,y + other.y)
    operator fun minus(other: Position) = Position(x - other.x,y - other.y)

    operator fun times(value: Double) = Position(x * value,y * value)
    operator fun times(value: Float) = Position(x * value,y * value)
    operator fun times(value: Int) = Position(x * value,y * value)
    operator fun div(value: Double) = Position(x / value,y / value)
    operator fun div(value: Float) = Position(x / value,y / value)
    operator fun div(value: Int) = Position(x / value,y / value)

    operator fun unaryMinus() = Position(-x,-y)

    fun pos(axis: Axis) = when(axis) {
        Axis.Horizontal -> x
        Axis.Vertical -> y
    }
}