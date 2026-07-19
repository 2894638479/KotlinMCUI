package io.github.u2894638479.kotlinmcui.math

import kotlin.test.*

class MeasureArrayTest {

    @Test
    fun `constructor from size initialises all to zero`() {
        val arr = MeasureArray(3)
        assertEquals(3, arr.size)
        assertEquals(0.px, arr[0])
        assertEquals(0.px, arr[1])
        assertEquals(0.px, arr[2])
    }

    @Test
    fun `constructor with init lambda`() {
        val arr = MeasureArray(3) { (it * 10).px }
        assertEquals(0.px, arr[0])
        assertEquals(10.px, arr[1])
        assertEquals(20.px, arr[2])
    }

    @Test
    fun `get and set roundtrip`() {
        val arr = MeasureArray(2)
        arr[0] = 42.px
        arr[1] = 99.px
        assertEquals(42.px, arr[0])
        assertEquals(99.px, arr[1])
    }

    @Test
    fun `firstOrNull returns null for empty - regression`() {
        val arr = MeasureArray(0)
        assertNull(arr.firstOrNull())
    }

    @Test
    fun `firstOrNull returns first element for non-empty - regression`() {
        val arr = MeasureArray(2) { (it * 5).px }
        val result = arr.firstOrNull()
        assertNotNull(result)
        assertEquals(0.px, result)
    }

    @Test
    fun `lastOrNull returns null for empty - regression`() {
        val arr = MeasureArray(0)
        assertNull(arr.lastOrNull())
    }

    @Test
    fun `lastOrNull returns last element for non-empty - regression`() {
        val arr = MeasureArray(3) { (it * 5).px }
        val result = arr.lastOrNull()
        assertNotNull(result)
        assertEquals(10.px, result)
    }

    @Test
    fun `getOrNull returns null for out of bounds`() {
        val arr = MeasureArray(2)
        assertNull(arr.getOrNull(-1))
        assertNull(arr.getOrNull(2))
    }

    @Test
    fun `getOrNull returns element for valid index`() {
        val arr = MeasureArray(2) { (it * 10).px }
        assertEquals(10.px, arr.getOrNull(1))
    }

    @Test
    fun `map transforms all elements`() {
        val arr = MeasureArray(3) { (it * 2).px }
        val result = arr.map { it * 2.0 }
        assertEquals(listOf(0.px, 4.px, 8.px), result)
    }

    @Test
    fun `sum computes total`() {
        val arr = MeasureArray(3) { (it + 1).px } // 1, 2, 3
        assertEquals(6.px, arr.sum())
    }

    @Test
    fun `average computes mean`() {
        val arr = MeasureArray(4) { (it * 10).px } // 0, 10, 20, 30
        assertEquals(15.px, arr.average())
    }

    @Test
    fun `isEmpty returns true for zero size`() {
        assertTrue(MeasureArray(0).isEmpty())
    }

    @Test
    fun `isNotEmpty returns true for non-zero size`() {
        assertTrue(MeasureArray(1).isNotEmpty())
    }

    @Test
    fun `asList returns view`() {
        val arr = MeasureArray(2) { (it * 5).px }
        val list = arr.asList()
        assertEquals(2, list.size)
        assertEquals(0.px, list[0])
        assertEquals(5.px, list[1])
    }

    @Test
    fun `first with predicate`() {
        val arr = MeasureArray(4) { (it * 10).px }
        val result = arr.asList().first { it > 5.px }
        assertEquals(10.px, result)
    }

    @Test
    fun `fill sets range to element`() {
        val arr = MeasureArray(3)
        arr.fill(42.px)
        assertEquals(42.px, arr[0])
        assertEquals(42.px, arr[1])
        assertEquals(42.px, arr[2])
    }

    @Test
    fun `any with predicate`() {
        val arr = MeasureArray(3) { (it * 10).px }
        assertTrue(arr.asList().any { it > 5.px })
        assertFalse(arr.asList().any { it > 50.px })
    }

    @Test
    fun `clone creates independent copy`() {
        val arr = MeasureArray(2) { (it * 10).px }
        val copy = arr.clone()
        assertEquals(arr[0], copy[0])
        assertEquals(arr[1], copy[1])

        copy[0] = 99.px
        assertNotEquals(arr[0], copy[0])
    }
}
