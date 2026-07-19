package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*

class RectTest {

    @Test
    fun `factory constructor sets all edges`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        assertEquals(1.px, r.left)
        assertEquals(2.px, r.top)
        assertEquals(10.px, r.right)
        assertEquals(20.px, r.bottom)
    }

    @Test
    fun `width computes right minus left`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        assertEquals(9.px, r.width)
    }

    @Test
    fun `height computes bottom minus top`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        assertEquals(18.px, r.height)
    }

    @Test
    fun `center computes midpoint`() {
        val r = Rect(0.px, 0.px, 10.px, 20.px)
        assertEquals(5.px, r.center.x)
        assertEquals(10.px, r.center.y)
    }

    @Test
    fun `contains point inside`() {
        val r = Rect(0.px, 0.px, 10.px, 10.px)
        assertTrue(r.contains(5.px, 5.px))
    }

    @Test
    fun `contains point on edge`() {
        val r = Rect(0.px, 0.px, 10.px, 10.px)
        assertTrue(r.contains(0.px, 0.px))
        assertTrue(r.contains(10.px, 10.px))
    }

    @Test
    fun `contains point outside`() {
        val r = Rect(0.px, 0.px, 10.px, 10.px)
        assertFalse(r.contains((-1).px, 5.px))
        assertFalse(r.contains(5.px, 11.px))
    }

    @Test
    fun `contains Position`() {
        val r = Rect(0.px, 0.px, 10.px, 10.px)
        assertTrue(Position(5.px, 5.px) in r)
        assertFalse(Position((-1).px, 5.px) in r)
    }

    @Test
    fun `contains rect fully inside`() {
        val outer = Rect(0.px, 0.px, 10.px, 10.px)
        val inner = Rect(2.px, 2.px, 8.px, 8.px)
        assertTrue(inner in outer)
    }

    @Test
    fun `contains rect partially overlapping`() {
        val outer = Rect(0.px, 0.px, 10.px, 10.px)
        val other = Rect(5.px, 5.px, 15.px, 15.px)
        assertFalse(other in outer)
    }

    @Test
    fun `overlap - fully overlapping`() {
        val a = Rect(0.px, 0.px, 10.px, 10.px)
        val b = Rect(5.px, 5.px, 15.px, 15.px)
        assertTrue(a.overlap(b))
    }

    @Test
    fun `overlap - disjoint horizontally`() {
        val a = Rect(0.px, 0.px, 10.px, 10.px)
        val b = Rect(20.px, 0.px, 30.px, 10.px)
        assertFalse(a.overlap(b))
    }

    @Test
    fun `overlap - disjoint vertically`() {
        val a = Rect(0.px, 0.px, 10.px, 10.px)
        val b = Rect(0.px, 20.px, 10.px, 30.px)
        assertFalse(a.overlap(b))
    }

    @Test
    fun `overlap - touching edges are overlapping`() {
        val a = Rect(0.px, 0.px, 10.px, 10.px)
        val b = Rect(10.px, 0.px, 20.px, 10.px)
        assertTrue(a.overlap(b))
    }

    @Test
    fun `expand increases all sides equally`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val expanded = r.expand(2.px)
        assertEquals((-1).px, expanded.left)
        assertEquals(0.px, expanded.top)
        assertEquals(12.px, expanded.right)
        assertEquals(22.px, expanded.bottom)
    }

    @Test
    fun `isEmpty returns true for zero width`() {
        val r = Rect(1.px, 1.px, 1.px, 10.px)
        assertTrue(r.isEmpty)
    }

    @Test
    fun `isEmpty returns true for zero height`() {
        val r = Rect(1.px, 1.px, 10.px, 1.px)
        assertTrue(r.isEmpty)
    }

    @Test
    fun `isEmpty returns false for positive area`() {
        val r = Rect(1.px, 1.px, 10.px, 10.px)
        assertFalse(r.isEmpty)
    }

    @Test
    fun `bound horizontal returns left to right`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val b = r.bound(Axis.Horizontal)
        assertEquals(1.px, b.low)
        assertEquals(10.px, b.high)
    }

    @Test
    fun `bound vertical returns top to bottom`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val b = r.bound(Axis.Vertical)
        assertEquals(2.px, b.low)
        assertEquals(20.px, b.high)
    }

    @Test
    fun `vertices returns 4 corners`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val v = r.vertices
        assertEquals(4, v.size)
        assertEquals(1.px, v[0].x); assertEquals(2.px, v[0].y)   // left-top
        assertEquals(10.px, v[1].x); assertEquals(2.px, v[1].y)  // right-top
        assertEquals(1.px, v[2].x); assertEquals(20.px, v[2].y)  // left-bottom
        assertEquals(10.px, v[3].x); assertEquals(20.px, v[3].y) // right-bottom
    }

    @Test
    fun `times creates scaled view`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val scaled = r * 2.0
        assertEquals(2.px, scaled.left)
        assertEquals(4.px, scaled.top)
        assertEquals(20.px, scaled.right)
        assertEquals(40.px, scaled.bottom)
    }

    @Test
    fun `fixed creates immutable copy that ignores writes`() {
        val r = Rect(1.px, 2.px, 10.px, 20.px)
        val fixed = r.fixed
        assertEquals(1.px, fixed.left)
        fixed.left = 100.px
        assertEquals(1.px, fixed.left) // write ignored
    }

    @Test
    fun `empty companion is zero-sized`() {
        assertEquals(0.px, Rect.empty.left)
        assertEquals(0.px, Rect.empty.top)
        assertEquals(0.px, Rect.empty.right)
        assertEquals(0.px, Rect.empty.bottom)
        assertTrue(Rect.empty.isEmpty)
    }
}
