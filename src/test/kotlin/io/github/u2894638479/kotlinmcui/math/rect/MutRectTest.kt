package io.github.u2894638479.kotlinmcui.math.rect

import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*

class MutRectTest {

    @Test
    fun `times write-back scales the source back`() {
        val rect = MutRect(2.px, 4.px, 6.px, 8.px)
        val scaled: MutRect = rect * 2.0
        assertEquals(4.px, scaled.left)
        assertEquals(16.px, scaled.bottom)

        scaled.left = 10.px
        assertEquals(5.px, rect.left, "writing through the view must reach the source rect")
    }

    @Test
    fun `expand write-back shrinks the source back`() {
        val rect = MutRect(10.px, 10.px, 20.px, 20.px)
        val expanded: MutRect = rect.expand(5.px)
        assertEquals(5.px, expanded.left)
        assertEquals(25.px, expanded.right)

        expanded.left = 7.px
        assertEquals(12.px, rect.left, "writing through the view must reach the source rect")
    }
}
