package io.github.u2894638479.kotlinmcui.scroll

import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*

class ScrollerTest {

    // Helper to create a scroller item
    private fun item(size: Measure, id: Int = 0): Scroller.Item = object : Scroller.Item {
        override val size = size
        override val identity = DslId(id)
    }

    // Helper to build a scroller with given items over a viewport
    private fun scrollerOf(
        items: List<Scroller.Item>,
        low: Measure = 0.px,
        high: Measure = 100.px,
        scale: Double = 1.0,
        offset: Double = 0.0,
        rawScroll: Double = 0.0,
        scroll: Double = 0.0,
        scrollIndex: Int = 0
    ): Scroller = object : Scroller {
        override val items = items
        override val low = low
        override val high = high
        override val scale = scale
        override var offset = offset
        override var rawScroll = rawScroll
        override var scroll = scroll
        override var scrollIndex = scrollIndex
    }

    @Test
    fun `calculateIndex at start returns index 0`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2), item(30.px, 3)),
            scroll = 0.0, offset = 0.0, scrollIndex = 0
        )
        val (index, offset) = s.calculateIndex(0.0)
        assertEquals(0, index)
        assertEquals(0.0, offset)
    }

    @Test
    fun `calculateIndex advances past first item`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2), item(30.px, 3)),
            scrollIndex = 0, offset = 0.0
        )
        // scroll past first item (10px)
        val (index, offset) = s.calculateIndex(15.0)
        assertEquals(1, index)
        assertEquals(10.0, offset)
    }

    @Test
    fun `updateIndex returns visible range`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2), item(30.px, 3), item(40.px, 4)),
            low = 0.px, high = 50.px, // viewport = 50px
            scrollIndex = 1, offset = 10.0, scroll = 10.0
        )
        val range = s.updateIndex()
        // viewport 0..50, starting at scroll=10.0, offset=10, item1=20px, item2=30px
        // scroll - offset = 0, items: 20 + 30 = 50 fills viewport
        // visible = items[1..2]
        assertEquals(1, range.first)
        assertEquals(2, range.last)
    }

    @Test
    fun `updateScroll clamps rawScroll to valid range`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2)),
            low = 0.px, high = 100.px, // viewport 100px > content 30px
            rawScroll = -10.0, scroll = -10.0, scrollIndex = 0, offset = 0.0
        )
        s.updateScroll()
        // total content = 30 < viewport 100, so rawScroll should be clamped to 0
        assertEquals(0.0, s.rawScroll)
    }

    @Test
    fun `isScrollable returns true when content exceeds viewport`() {
        val s = scrollerOf(
            items = listOf(item(60.px, 1), item(60.px, 2)), // 120px content
            low = 0.px, high = 100.px // 100px viewport
        )
        assertTrue(s.isScrollable())
    }

    @Test
    fun `isScrollable returns false when content fits viewport`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2)), // 30px content
            low = 0.px, high = 100.px // 100px viewport
        )
        assertFalse(s.isScrollable())
    }

    @Test
    fun `scroll returns unconsumed amount`() {
        val s = scrollerOf(
            items = listOf(item(30.px, 1), item(30.px, 2)),
            low = 0.px, high = 50.px,
            rawScroll = 0.0, scroll = 0.0, scrollIndex = 0, offset = 0.0
        )
        val unconsumed = s.scroll(10.0)
        assertEquals(0.0, unconsumed, "scrolling 10px within 60px content should consume all")
        assertEquals(10.0, s.rawScroll)
    }

    @Test
    fun `empty scroller does nothing`() {
        val s = Scroller.empty
        assertEquals(0.px, s.low)
        assertEquals(0.px, s.high)
        assertTrue(s.items.isEmpty())
        assertFalse(s.isScrollable()) // content = 0, viewport = 0
    }

    // ── calculateIndex: the backward walk ────────────────────────────────

    /**
     * Every other `calculateIndex` case starts at `scrollIndex = 0, offset = 0`, so
     * the `while (scroll - offset < 0)` loop body never runs. Walking backwards must
     * *subtract* each item it steps over; adding instead overshoots to index 0.
     */
    @Test
    fun `calculateIndex walks backwards from a nonzero index and offset`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2), item(30.px, 3), item(40.px, 4)),
            low = 0.px, high = 50.px,
            scrollIndex = 3, offset = 60.0, rawScroll = 60.0, scroll = 60.0
        )
        // item3 starts at 10+20+30 = 60; scrolling back to 15 lands inside item1 (10..30).
        val (index, offset) = s.calculateIndex(15.0)
        assertEquals(1, index, "15px lies inside item1, whose top is at 10px")
        assertEquals(10.0, offset, "offset must be the cumulative size of the items before the index")
    }

    /** The backward walk must stop at the first item instead of running past it. */
    @Test
    fun `calculateIndex clamps a backward walk at the first item`() {
        val s = scrollerOf(
            items = listOf(item(10.px, 1), item(20.px, 2), item(30.px, 3)),
            low = 0.px, high = 50.px,
            scrollIndex = 2, offset = 30.0, rawScroll = 30.0, scroll = 30.0
        )
        val (index, offset) = s.calculateIndex((-5.0))
        assertEquals(0, index)
        assertEquals(0.0, offset)
    }

    // ── ScrollConstraint ────────────────────────────────────────────────

    private fun twentyItems(low: Measure = 0.px, high: Measure = 100.px) = scrollerOf(
        items = List(20) { item(10.px, it) },
        low = low, high = high,
        scrollIndex = 0, offset = 0.0, rawScroll = 0.0, scroll = 0.0
    )

    /**
     * `KeepIn(padding)` must reserve the padding on **both** edges of the viewport.
     * Item 9 spans 90..100 in a 100px viewport, so it is already technically visible;
     * with a 10px inset its bottom edge violates the inset and it must be pushed up
     * by exactly 10px.
     */
    @Test
    fun `scrollTo with KeepIn keeps the item inside the padded viewport`() {
        val s = twentyItems()
        s.scrollTo(9, ScrollConstraint.KeepIn(10.px))
        assertEquals(10.0, s.rawScroll, "a 10px inset must push item 9 (90..100) up by 10px")
    }

    /** An item that already sits fully inside the padded viewport must not move. */
    @Test
    fun `scrollTo with KeepIn does not move an already visible item`() {
        val s = twentyItems()
        s.scrollTo(5, ScrollConstraint.KeepIn(10.px))
        assertEquals(0.0, s.rawScroll, "item 5 (50..60) is inside 10..90 and must stay put")
    }

    /**
     * `KeepInRatio(ratio)` insets by a **fraction of the viewport**, not by the raw
     * ratio. Item 8 spans 80..90; with ratio 0.25 over a 100px viewport the usable
     * band is 25..75, so the item is pushed down by exactly 15px. Dropping the
     * viewport factor makes the pad 0.25px and the item look visible.
     */
    @Test
    fun `scrollTo with KeepInRatio insets by a fraction of the viewport`() {
        val s = twentyItems()
        s.scrollTo(8, ScrollConstraint.KeepInRatio(0.25))
        assertEquals(15.0, s.rawScroll, "the 25px inset must push item 8 (80..90) up by 15px")
    }

    /** A ratio of 0 means "any part visible counts", so item 9 must not move. */
    @Test
    fun `scrollTo with KeepInRatio of zero does not move a partially visible item`() {
        val s = twentyItems()
        s.scrollTo(9, ScrollConstraint.KeepInRatio(0.0))
        assertEquals(0.0, s.rawScroll)
    }

    /**
     * `AlignTo` always re-aligns, and the three [Align] modes must land on three
     * different scroll positions. Item 10 spans 100..110 in a 200px-tall content and
     * a 100px viewport: LOW leaves its top at the viewport top (100), MID centres it
     * (55) and HIGH puts its bottom at the viewport bottom (10).
     */
    @Test
    fun `scrollTo with AlignTo honours LOW MID and HIGH separately`() {
        fun fresh() = twentyItems()
        val low = fresh().also { it.scrollTo(10, ScrollConstraint.AlignTo(Align.LOW)) }.rawScroll
        val mid = fresh().also { it.scrollTo(10, ScrollConstraint.AlignTo(Align.MID)) }.rawScroll
        val high = fresh().also { it.scrollTo(10, ScrollConstraint.AlignTo(Align.HIGH)) }.rawScroll

        assertEquals(100.0, low, "LOW puts the item top at the viewport top")
        assertEquals(55.0, mid, "MID centres the item in the viewport")
        assertEquals(10.0, high, "HIGH puts the item bottom at the viewport bottom")
    }

    /** `AlignTo` on an out-of-range index must be a no-op, like the other overloads. */
    @Test
    fun `scrollTo with a constraint ignores an out of range index`() {
        val s = twentyItems()
        s.scrollTo(99, ScrollConstraint.KeepIn(10.px))
        s.scrollTo((-1), ScrollConstraint.AlignTo(Align.HIGH))
        assertEquals(0.0, s.rawScroll)
    }
}
