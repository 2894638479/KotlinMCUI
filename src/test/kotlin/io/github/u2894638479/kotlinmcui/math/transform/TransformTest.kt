package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.*

class TransformTest {

    /** Compare two Measures by their raw Double values with a tolerance. */
    private fun assertMeasureEquals(expected: Measure, actual: Measure, tolerance: Double = 0.01) {
        val diff = (expected - actual).absoluteValue
        assertTrue(diff < tolerance.px || !diff.isNumber,
            "expected $expected but was $actual (diff $diff)")
    }

    @Test
    fun `empty is identity matrix`() {
        val t = Transform.empty
        assertTrue(t.isEmpty)
    }

    @Test
    fun `identity transform does not change position`() {
        val t = Transform.empty
        val pos = Position(10.px, 20.px)
        val result = t * pos
        assertEquals(10.px, result.x)
        assertEquals(20.px, result.y)
    }

    @Test
    fun `translate moves position`() {
        val t = Transform.translate(5.px, 10.px)
        val pos = Position(1.px, 2.px)
        val result = t * pos
        assertEquals(6.px, result.x)
        assertEquals(12.px, result.y)
    }

    @Test
    fun `scale around origin`() {
        val t = Transform.scale(0.px, 0.px, 2.0, 3.0)
        val pos = Position(5.px, 5.px)
        val result = t * pos
        assertMeasureEquals(10.px, result.x)
        assertMeasureEquals(15.px, result.y)
    }

    @Test
    fun `rotate 90 degrees around origin`() {
        val t = Transform.rotate(0.px, 0.px, PI / 2)
        val pos = Position(1.px, 0.px)
        val result = t * pos
        assertMeasureEquals(0.px, result.x)
        assertMeasureEquals(1.px, result.y)
    }

    @Test
    fun `rotate 180 degrees around origin`() {
        val t = Transform.rotate(0.px, 0.px, PI)
        val pos = Position(1.px, 2.px)
        val result = t * pos
        assertMeasureEquals((-1.0).px, result.x)
        assertMeasureEquals((-2.0).px, result.y)
    }

    @Test
    fun `multiply two transforms`() {
        val t1 = Transform.translate(5.px, 0.px)
        val t2 = Transform.translate(0.px, 10.px)
        val composed = t1 * t2
        val pos = Position(0.px, 0.px)
        val result = composed * pos
        assertEquals(5.px, result.x)
        assertEquals(10.px, result.y)
    }

    @Test
    fun `invert roundtrip returns identity`() {
        val t = Transform.translate(5.px, 10.px) * Transform.scale(0.px, 0.px, 2.0, 2.0)
        val inv = t.invert()
        val composed = t * inv
        val pos = Position(3.px, 7.px)
        val result = composed * pos
        assertMeasureEquals(3.px, result.x)
        assertMeasureEquals(7.px, result.y)
    }

    @Test
    fun `isEmpty detects non-empty transform`() {
        val t = Transform.translate(1.px, 1.px)
        assertFalse(t.isEmpty)
    }

    @Test
    fun `multiply with empty is optimised`() {
        val t = Transform.translate(5.px, 5.px)
        val pos = Position(1.px, 1.px)
        val expected = t * pos
        val result1 = (t * Transform.empty) * pos
        val result2 = (Transform.empty * t) * pos
        assertEquals(expected.x, result1.x)
        assertEquals(expected.y, result1.y)
        assertEquals(expected.x, result2.x)
        assertEquals(expected.y, result2.y)
    }

    @Test
    fun `scale around center point`() {
        val t = Transform.scale(10.px, 10.px, 2.0, 2.0)
        val pos = Position(15.px, 15.px) // 5 px away from center
        val result = t * pos
        assertMeasureEquals(20.px, result.x) // center + (5 * 2) = 20
        assertMeasureEquals(20.px, result.y)

        val center = Position(10.px, 10.px)
        val centerResult = t * center
        assertMeasureEquals(10.px, centerResult.x) // center stays put
        assertMeasureEquals(10.px, centerResult.y)
    }
}
