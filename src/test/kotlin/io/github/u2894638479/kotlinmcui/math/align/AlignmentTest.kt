package io.github.u2894638479.kotlinmcui.math.align

import io.github.u2894638479.kotlinmcui.math.Axis
import kotlin.test.*

class AlignmentTest {

    @Test
    fun `default is MID MID`() {
        val a = Alignment()
        assertEquals(Align.MID, a.horizontal)
        assertEquals(Align.MID, a.vertical)
    }

    @Test
    fun `horizontal and vertical extractors`() {
        val a = Alignment().left().top()
        assertEquals(Align.LOW, a.horizontal)
        assertEquals(Align.LOW, a.vertical)
    }

    @Test
    fun `axis returns correct Align`() {
        val a = Alignment().left().top()
        assertEquals(Align.LOW, a.axis(Axis.Horizontal))
        assertEquals(Align.LOW, a.axis(Axis.Vertical))
    }

    @Test
    fun `left sets horizontal to LOW`() {
        assertEquals(Align.LOW, Alignment().left().horizontal)
    }

    @Test
    fun `right sets horizontal to HIGH`() {
        assertEquals(Align.HIGH, Alignment().right().horizontal)
    }

    @Test
    fun `top sets vertical to LOW`() {
        assertEquals(Align.LOW, Alignment().top().vertical)
    }

    @Test
    fun `bottom sets vertical to HIGH`() {
        assertEquals(Align.HIGH, Alignment().bottom().vertical)
    }

    @Test
    fun `middleX sets horizontal to MID`() {
        assertEquals(Align.MID, Alignment().left().middleX().horizontal)
    }

    @Test
    fun `middleY sets vertical to MID`() {
        assertEquals(Align.MID, Alignment().top().middleY().vertical)
    }

    @Test
    fun `middle sets both to MID`() {
        val a = Alignment().left().top().middle()
        assertEquals(Align.MID, a.horizontal)
        assertEquals(Align.MID, a.vertical)
    }

    @Test
    fun `fluent chaining preserves unchanged axis`() {
        val a = Alignment().left().top().middleX()
        assertEquals(Align.MID, a.horizontal)
        assertEquals(Align.LOW, a.vertical) // unchanged by middleX
    }

    @Test
    fun `horizontal setter maintains vertical`() {
        val a = Alignment().top().right()
        assertEquals(Align.HIGH, a.horizontal)
        assertEquals(Align.LOW, a.vertical)
    }

    @Test
    fun `vertical setter maintains horizontal`() {
        val a = Alignment().left().bottom()
        assertEquals(Align.LOW, a.horizontal)
        assertEquals(Align.HIGH, a.vertical)
    }
}
