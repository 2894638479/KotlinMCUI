package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.identity.DslId
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
}
