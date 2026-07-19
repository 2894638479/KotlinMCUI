package io.github.u2894638479.kotlinmcui.math

import kotlin.test.*

class PositionTest {

    @Test
    fun `constructor sets x and y`() {
        val p = Position(3.0.px, 4.0.px)
        assertEquals(3.0.px, p.x)
        assertEquals(4.0.px, p.y)
    }

    @Test
    fun `plus adds components`() {
        val a = Position(1.0.px, 2.0.px)
        val b = Position(3.0.px, 4.0.px)
        val result = a + b
        assertEquals(4.0.px, result.x)
        assertEquals(6.0.px, result.y)
    }

    @Test
    fun `minus subtracts components`() {
        val a = Position(5.0.px, 7.0.px)
        val b = Position(3.0.px, 4.0.px)
        val result = a - b
        assertEquals(2.0.px, result.x)
        assertEquals(3.0.px, result.y)
    }

    @Test
    fun `times with Double scales both components`() {
        val p = Position(3.0.px, 4.0.px)
        val result = p * 2.0
        assertEquals(6.0.px, result.x)
        assertEquals(8.0.px, result.y)
    }

    @Test
    fun `div with Double scales both components`() {
        val p = Position(6.0.px, 8.0.px)
        val result = p / 2.0
        assertEquals(3.0.px, result.x)
        assertEquals(4.0.px, result.y)
    }

    @Test
    fun `unaryMinus negates both components`() {
        val p = Position(3.0.px, (-4.0).px)
        val result = -p
        assertEquals((-3.0).px, result.x)
        assertEquals(4.0.px, result.y)
    }

    @Test
    fun `get with Axis Horizontal returns x`() {
        val p = Position(3.0.px, 4.0.px)
        assertEquals(3.0.px, p[Axis.Horizontal])
    }

    @Test
    fun `get with Axis Vertical returns y`() {
        val p = Position(3.0.px, 4.0.px)
        assertEquals(4.0.px, p[Axis.Vertical])
    }
}
