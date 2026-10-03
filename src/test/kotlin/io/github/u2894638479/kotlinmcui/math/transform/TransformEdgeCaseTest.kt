package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*

class TransformEdgeCaseTest {

    @Test
    fun `identity factories return the shared empty transform`() {
        assertSame(Transform.empty, Transform.translate(0.px, 0.px))
        assertSame(Transform.empty, Transform.scale(Position(0.px, 0.px), 1.0, 1.0))
        assertSame(Transform.empty, Transform.rotate(Position(0.px, 0.px), 0.0))
        assertSame(Transform.empty, Transform.shear(Position(0.px, 0.px), 0.0, 0.0))
        assertSame(Transform.empty, Transform.perspective(0.0, 0.0))
        assertSame(Transform.empty, Transform.perspective(Position(0.px, 0.px), 0.0, 0.0))
    }

    @Test
    fun `a real transform is not reported as empty`() {
        assertFalse(Transform.translate(1.px, 0.px).isEmpty)
        assertFalse(Transform.rotate(Position(0.px, 0.px), 0.5).isEmpty)
    }

    @Test
    fun `inverting a singular transform yields the identity instead of NaN`() {
        val singular = Transform.scale(Position(10.px, 10.px), 0.0, 0.0)
        val inverse = singular.invert()
        assertTrue(inverse.isEmpty, "a singular matrix has no inverse; the identity is the safe fallback")

        val point = inverse * Position(3.px, 4.px)
        assertEquals(3.px, point.x)
        assertEquals(4.px, point.y)
    }

    @Test
    fun `composing the empty transform is a no-op`() {
        val t = Transform.translate(5.px, 0.px)
        assertSame(t, Transform.empty * t)
        assertSame(t, t * Transform.empty)
    }
}
