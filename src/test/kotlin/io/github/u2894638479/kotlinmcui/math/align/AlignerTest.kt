package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Bound
import kotlin.test.*

class AlignerTest {

    // --- simplePlace ---

    @Test
    fun `simplePlace places each item independently based on its align`() {
        val items = listOf(
            TestAlignable(size = 10.px, align = Align.LOW),
            TestAlignable(size = 20.px, align = Align.LOW),
            TestAlignable(size = 30.px, align = Align.MID)
        )
        Aligner.simplePlace.align(0.px, 100.px, items)
        // All LOW items start at low=0
        assertEquals(0.px, items[0].low)
        assertEquals(10.px, items[0].high)
        assertEquals(0.px, items[1].low)
        assertEquals(20.px, items[1].high)
        // MID item is centered: (100 - 30) / 2 = 35
        assertEquals(35.px, items[2].low)
        assertEquals(65.px, items[2].high)
    }

    // --- equalSpacing ---

    @Test
    fun `equalSpacing distributes items with even gaps`() {
        val items = listOf(
            TestAlignable(size = 10.px),
            TestAlignable(size = 10.px)
        )
        // total size = 20, remaining = 80, gap = 40
        Aligner.equalSpacing.align(0.px, 100.px, items)
        assertEquals(20.px, items[0].low)
        assertEquals(30.px, items[0].high)
        assertEquals(70.px, items[1].low)
        assertEquals(80.px, items[1].high)
    }

    @Test
    fun `equalSpacing with empty list does nothing`() {
        Aligner.equalSpacing.align(0.px, 100.px, emptyList())
        // no exception thrown
    }

    // --- close ---

    @Test
    fun `close LOW packs items at start`() {
        val items = listOf(
            TestAlignable(size = 10.px),
            TestAlignable(size = 20.px)
        )
        Aligner.close(Align.LOW).align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(10.px, items[0].high)
        assertEquals(10.px, items[1].low)
        assertEquals(30.px, items[1].high)
    }

    @Test
    fun `close MID centers items`() {
        val items = listOf(
            TestAlignable(size = 10.px),
            TestAlignable(size = 20.px)
        )
        // total = 30, start = (100-30)/2 = 35
        Aligner.close(Align.MID).align(0.px, 100.px, items)
        assertEquals(35.px, items[0].low)
        assertEquals(45.px, items[0].high)
        assertEquals(45.px, items[1].low)
        assertEquals(65.px, items[1].high)
    }

    @Test
    fun `close HIGH packs items at end`() {
        val items = listOf(
            TestAlignable(size = 10.px),
            TestAlignable(size = 20.px)
        )
        Aligner.close(Align.HIGH).align(0.px, 100.px, items)
        assertEquals(70.px, items[0].low)
        assertEquals(80.px, items[0].high)
        assertEquals(80.px, items[1].low)
        assertEquals(100.px, items[1].high)
    }

    // --- weightedStrictByMin ---

    @Test
    fun `weightedStrictByMin fixed items get exact size`() {
        val items = listOf(
            TestAlignable(size = 10.px, weight = 1.0),
            TestAlignable(size = 20.px, weight = 1.0)
        )
        Aligner.weightedStrictByMin.align(0.px, 30.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(10.px, items[0].high)
        assertEquals(10.px, items[1].low)
        assertEquals(30.px, items[1].high)
    }

    @Test
    fun `weightedStrictByMin auto items share remaining space by weight`() {
        val items = listOf(
            TestAlignable(size = Measure.AUTO, weight = 1.0, minSize = 0.px),
            TestAlignable(size = Measure.AUTO, weight = 2.0, minSize = 0.px)
        )
        // sum autoSizeMin = 0, remain = 100, sumWeight = 3.0
        Aligner.weightedStrictByMin.align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertTrue(items[0].high > 30.px && items[0].high < 40.px)
        assertEquals(items[0].high, items[1].low)
        assertEquals(100.px, items[1].high)
    }

    // --- weightedExtend ---

    @Test
    fun `weightedExtend fixed items take their exact size`() {
        val items = listOf(
            TestAlignable(size = 10.px, weight = 1.0),
            TestAlignable(size = Measure.AUTO, weight = 1.0, minSize = 0.px)
        )
        Aligner.weightedExtend.align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(10.px, items[0].high)
        assertEquals(10.px, items[1].low)
        assertEquals(100.px, items[1].high)
    }

    // --- equalWeightedExtend ---

    @Test
    fun `equalWeightedExtend auto items share remaining space equally`() {
        val items = listOf(
            TestAlignable(size = Measure.AUTO, minSize = 0.px),
            TestAlignable(size = Measure.AUTO, minSize = 0.px)
        )
        Aligner.equalWeightedExtend.align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(50.px, items[0].high)
        assertEquals(50.px, items[1].low)
        assertEquals(100.px, items[1].high)
    }

    @Test
    fun `equalWeightedExtend fixed items get their autoSizeMin - regression for bug fix`() {
        val items = listOf(
            TestAlignable(size = 20.px, minSize = 0.px),
            TestAlignable(size = Measure.AUTO, minSize = 0.px)
        )
        Aligner.equalWeightedExtend.align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(20.px, items[0].high, "fixed-size item should get its autoSizeMin, not zero")
        assertEquals(20.px, items[1].low)
        assertEquals(100.px, items[1].high)
    }

    @Test
    fun `equalWeightedExtend with no auto items gives all fixed items correct size`() {
        val items = listOf(
            TestAlignable(size = 10.px),
            TestAlignable(size = 20.px)
        )
        Aligner.equalWeightedExtend.align(0.px, 100.px, items)
        assertEquals(0.px, items[0].low)
        assertEquals(10.px, items[0].high, "fixed-size item should get autoSizeMin = size")
        assertEquals(10.px, items[1].low)
        assertEquals(30.px, items[1].high, "fixed-size item should get autoSizeMin = size")
    }

    // --- align(bound) extension ---

    @Test
    fun `align with Bound delegates correctly`() {
        val items = listOf(TestAlignable(size = 10.px))
        val bound = object : Bound {
            override val low = 10.px
            override val high = 50.px
        }
        Aligner.close(Align.LOW).align(bound, items)
        assertEquals(10.px, items[0].low)
        assertEquals(20.px, items[0].high)
    }
}
