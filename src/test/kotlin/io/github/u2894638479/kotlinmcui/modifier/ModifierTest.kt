package io.github.u2894638479.kotlinmcui.modifier

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*

class ModifierTest {

    @Test
    fun `companion default values`() {
        assertEquals(Measure.AUTO, Modifier.width)
        assertEquals(Measure.AUTO, Modifier.height)
        assertEquals(1.0, Modifier.weight)
        assertEquals(0.px, Modifier.paddingTop)
        assertEquals(0.px, Modifier.paddingLeft)
        assertEquals(0.px, Modifier.paddingBottom)
        assertEquals(0.px, Modifier.paddingRight)
        assertEquals(0.px, Modifier.minWidth)
        assertEquals(0.px, Modifier.minHeight)
    }

    @Test
    fun `size sets width and height`() {
        val m = Modifier.size(10.px, 20.px)
        assertEquals(10.px, m.width)
        assertEquals(20.px, m.height)
    }

    @Test
    fun `width sets only width`() {
        val m = Modifier width 10.px
        assertEquals(10.px, m.width)
        assertEquals(Measure.AUTO, m.height) // unchanged
    }

    @Test
    fun `height sets only height`() {
        val m = Modifier height 20.px
        assertEquals(Measure.AUTO, m.width) // unchanged
        assertEquals(20.px, m.height)
    }

    @Test
    fun `minWidth takes max with existing`() {
        val m = Modifier.minWidth(5.px).minWidth(10.px)
        assertEquals(10.px, m.minWidth)
    }

    @Test
    fun `minHeight takes max with existing`() {
        val m = Modifier.minHeight(8.px).minHeight(3.px)
        assertEquals(8.px, m.minHeight)
    }

    @Test
    fun `weight overrides value`() {
        val m = Modifier weight 2.5
        assertEquals(2.5, m.weight)
    }

    @Test
    fun `padding single value sets all sides`() {
        val m = Modifier padding 5.px
        assertEquals(5.px, m.paddingLeft)
        assertEquals(5.px, m.paddingTop)
        assertEquals(5.px, m.paddingRight)
        assertEquals(5.px, m.paddingBottom)
    }

    @Test
    fun `padding h and v`() {
        val m = Modifier.padding(h = 3.px, v = 5.px)
        assertEquals(3.px, m.paddingLeft)
        assertEquals(5.px, m.paddingTop)
        assertEquals(3.px, m.paddingRight)
        assertEquals(5.px, m.paddingBottom)
    }

    @Test
    fun `padding individual sides`() {
        val m = Modifier.padding(left = 1.px, top = 2.px, right = 3.px, bottom = 4.px)
        assertEquals(1.px, m.paddingLeft)
        assertEquals(2.px, m.paddingTop)
        assertEquals(3.px, m.paddingRight)
        assertEquals(4.px, m.paddingBottom)
    }

    @Test
    fun `padding accumulates across chain`() {
        val m = Modifier padding 5.px padding 3.px
        assertEquals(8.px, m.paddingLeft)
        assertEquals(8.px, m.paddingTop)
    }

    @Test
    fun `paddingWidth sums left and right`() {
        val m = Modifier.padding(left = 3.px, right = 5.px)
        assertEquals(8.px, m.paddingWidth)
    }

    @Test
    fun `paddingHeight sums top and bottom`() {
        val m = Modifier.padding(top = 2.px, bottom = 6.px)
        assertEquals(8.px, m.paddingHeight)
    }

    @Test
    fun `contentMinWidth returns width when it is a number`() {
        val m = Modifier width 50.px
        assertEquals(50.px, m.contentMinWidth)
    }

    @Test
    fun `contentMinWidth falls back to minWidth when width is AUTO`() {
        val m = Modifier.minWidth(10.px)
        // width is AUTO by default, so contentMinWidth = minWidth
        assertFalse(Modifier.width.isNumber)
        assertEquals(10.px, m.contentMinWidth)
    }

    @Test
    fun `contentMinHeight returns height when it is a number`() {
        val m = Modifier height 30.px
        assertEquals(30.px, m.contentMinHeight)
    }

    @Test
    fun `contentMinHeight falls back to minHeight when height is AUTO`() {
        val m = Modifier.minHeight(15.px)
        assertFalse(Modifier.height.isNumber)
        assertEquals(15.px, m.contentMinHeight)
    }
}
