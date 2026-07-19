package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.context.DslScaleContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.context.unscaled
import kotlin.test.*

class MeasureTest {

    @Test
    fun `NaN sentinel - isNumber returns false for AUTO`() {
        assertFalse(Measure.AUTO.isNumber)
    }

    @Test
    fun `NaN sentinel - isNumber returns false for AUTO_MIN`() {
        assertFalse(Measure.AUTO_MIN.isNumber)
    }

    @Test
    fun `NaN sentinel - AUTO is signaling NaN not quiet NaN`() {
        assertFalse(Measure.AUTO.isQNaN)
    }

    @Test
    fun `NaN sentinel - AUTO_MIN is signaling NaN not quiet NaN`() {
        assertFalse(Measure.AUTO_MIN.isQNaN)
    }

    @Test
    fun `NaN sentinel - isAutoMin returns true only for AUTO_MIN`() {
        assertTrue(Measure.AUTO_MIN.isAutoMin)
        assertFalse(Measure.AUTO.isAutoMin)
    }

    @Test
    fun `NaN sentinel - isFinite returns false for AUTO`() {
        assertFalse(Measure.AUTO.isFinite)
    }

    @Test
    fun `px extension - Double to Measure`() {
        val m = 10.0.px
        assertEquals(10.0, m.pixelsOrElse { fail() })
    }

    @Test
    fun `px extension - Float to Measure`() {
        val m = 5f.px
        assertEquals(5.0, m.pixelsOrElse { fail() })
    }

    @Test
    fun `px extension - Int to Measure`() {
        val m = 3.px
        assertEquals(3.0, m.pixelsOrElse { fail() })
    }

    @Test
    fun `arithmetic - plus with two numbers`() {
        assertEquals(7.0.px, 3.0.px + 4.0.px)
    }

    @Test
    fun `arithmetic - minus with two numbers`() {
        assertEquals(1.0.px, 5.0.px - 4.0.px)
    }

    @Test
    fun `arithmetic - times with Double`() {
        assertEquals(6.0.px, 3.0.px * 2.0)
    }

    @Test
    fun `arithmetic - div with Double`() {
        assertEquals(3.0.px, 6.0.px / 2.0)
    }

    @Test
    fun `arithmetic - unaryMinus`() {
        assertEquals((-3.0).px, -3.0.px)
    }

    @Test
    fun `arithmetic - plus propagates NaN`() {
        val result = Measure.AUTO + 3.0.px
        assertFalse(result.isNumber)
    }

    @Test
    fun `arithmetic - times propagates NaN`() {
        val result = Measure.AUTO * 2.0
        assertFalse(result.isNumber)
    }

    @Test
    fun `companion - max selects larger value`() {
        assertEquals(5.0.px, Measure.max(3.0.px, 5.0.px))
    }

    @Test
    fun `companion - min selects smaller value`() {
        assertEquals(3.0.px, Measure.min(3.0.px, 5.0.px))
    }

    @Test
    fun `ifNan - returns self when number`() {
        val result = 5.0.px.ifNan { 0.px }
        assertEquals(5.0.px, result)
    }

    @Test
    fun `ifNan - falls back when NaN`() {
        val result = Measure.AUTO.ifNan { 0.px }
        assertEquals(0.px, result)
    }

    @Test
    fun `pixelsOrElse - converts to Int when number`() {
        val result = 42.0.px.pixelsOrElse<Int> { fail() }
        assertEquals(42, result)
    }

    @Test
    fun `pixelsOrElse - returns default when NaN`() {
        val result = Measure.AUTO.pixelsOrElse<Int> { 99 }
        assertEquals(99, result)
    }

    @Test
    fun `pixels - returns Int when number`() {
        val result = 42.0.px.pixelsOrElse<Int?> { null }
        assertEquals(42, result)
    }

    @Test
    fun `pixels - returns null when NaN`() {
        val result = Measure.AUTO.pixelsOrElse<Int?> { null }
        assertNull(result)
    }

    @Test
    fun `absoluteValue - of positive`() {
        assertEquals(3.0.px, 3.0.px.absoluteValue)
    }

    @Test
    fun `absoluteValue - of negative`() {
        assertEquals(3.0.px, (-3.0).px.absoluteValue)
    }

    @Test
    fun `compareTo - larger is greater`() {
        assertTrue(5.0.px > 3.0.px)
    }

    @Test
    fun `compareTo - equal values`() {
        assertEquals(0, 3.0.px.compareTo(3.0.px))
    }

    @Test
    fun `div with Measure returns ratio`() {
        assertEquals(2.0, 6.0.px / 3.0.px)
    }

    @Test
    fun `sumOf - Iterable extension`() {
        val list = listOf(1.0, 2.0, 3.0)
        assertEquals(6.0.px, list.sumOf { it.px })
    }

    @Test
    fun `minOf - Iterable extension`() {
        val list = listOf(3.0, 1.0, 2.0)
        assertEquals(1.0.px, list.minOf { it.px })
    }

    @Test
    fun `maxOf - Iterable extension`() {
        val list = listOf(3.0, 1.0, 2.0)
        assertEquals(3.0.px, list.maxOf { it.px })
    }

    @Test
    fun `scaled extension - multiplies by scale factor`() {
        val ctx = object : DslScaleContext {
            override val scale = 2.0
        }
        context(ctx) {
            assertEquals(20.0.px, 10.0.scaled)
        }
    }

    @Test
    fun `unscaled extension - divides by scale factor`() {
        val ctx = object : DslScaleContext {
            override val scale = 2.0
        }
        context(ctx) {
            assertEquals(5.0, 10.0.px.unscaled)
        }
    }
}
