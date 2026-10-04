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
        // The guard has to come first: `!diff.isNumber` is true for NaN, so the old
        // `diff < tolerance || !diff.isNumber` form accepted every NaN result silently.
        assertTrue(diff.isNumber, "expected a real number but was $actual")
        assertTrue(diff < tolerance.px, "expected $expected but was $actual (diff $diff)")
    }

    private val allEntries: List<(Transform) -> Float> = listOf(
        { it.m00 }, { it.m01 }, { it.m02 },
        { it.m10 }, { it.m11 }, { it.m12 },
        { it.m20 }, { it.m21 }, { it.m22 },
    )

    /** Compare two matrices entry by entry — the homogeneous divide cannot hide a scale. */
    private fun assertMatrixEquals(expected: Transform, actual: Transform, tolerance: Float = 1e-3f) {
        allEntries.forEachIndexed { i, entry ->
            val e = entry(expected)
            val a = entry(actual)
            assertTrue(abs(e - a) < tolerance, "entry $i: expected $e but was $a")
        }
    }

    /**
     * A matrix that is neither upper triangular (`m10`, `m20` and `m21` are all
     * nonzero) nor a uniform scale, with a non-trivial translation. Every cofactor
     * of [Transform.invert] contributes to the product, so a wrong cofactor cannot
     * cancel out.
     */
    private val skewed = Transform(
        1.0f, 0.5f, 5.0f,
        0.25f, 2.0f, 10.0f,
        0.02f, 0.03f, 1.0f
    )

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

    /**
     * The inverse of a non-upper-triangular matrix must compose back to *exactly*
     * the identity matrix.
     *
     * The old round-trip in `invert roundtrip returns identity` applies the product
     * to a [Position], and [Transform] divides by the homogeneous `w` afterwards —
     * so any uniform scaling of the inverse (e.g. dropping the `1 / det` factor and
     * leaving an inverse 1.5125x too large) cancels out and the assertion cannot
     * see it. Comparing the nine matrix entries keeps that scale visible.
     */
    @Test
    fun `invert roundtrip of a skewed matrix is the identity matrix`() {
        val inv = skewed.invert()
        assertFalse(inv.isEmpty, "a skewed matrix has a nonzero determinant and must be invertible")
        assertMatrixEquals(Transform.empty, skewed * inv, tolerance = 1e-3f)
        assertMatrixEquals(Transform.empty, inv * skewed, tolerance = 1e-3f)
    }

    /** The same round trip through a position, for the case where `w` is not 1. */
    @Test
    fun `invert roundtrip of a skewed matrix maps positions back`() {
        val pos = Position(12.px, (-7).px)
        val round = (skewed * skewed.invert()) * pos
        assertMeasureEquals(pos.x, round.x)
        assertMeasureEquals(pos.y, round.y)
    }

    /** A singular matrix must still collapse to the identity rather than to NaN. */
    @Test
    fun `invert of a singular matrix returns the identity`() {
        val singular = Transform.scale(0.px, 0.px, 0.0, 1.0)
        assertTrue(singular.invert().isEmpty)
    }
}
