package io.github.u2894638479.kotlinmcui.identity

import io.github.u2894638479.kotlinmcui.identity.DslId
import kotlin.test.*

class DslIdTest {

    @Test
    fun `chain built with plus operator`() {
        val root = DslId("root")
        val child = root + "child"
        val grandchild = child + "grandchild"

        assertEquals(3, grandchild.size)
        assertEquals("grandchild", grandchild.topElement)
    }

    @Test
    fun `equality compares full chain`() {
        val a = DslId("a")
        val b = DslId("a")
        val a1 = a + "child"
        val b1 = b + "child"

        assertEquals(a, b)
        assertEquals(a1, b1)
        assertNotEquals(a, a1)
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = DslId("x") + "y"
        val b = DslId("x") + "y"
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `size returns depth`() {
        assertEquals(1, DslId("a").size)
        assertEquals(2, (DslId("a") + "b").size)
        assertEquals(3, (DslId("a") + "b" + "c").size)
    }

    @Test
    fun `topElement returns most recently added object`() {
        val id = DslId("a") + "b" + "c"
        assertEquals("c", id.topElement)
    }

    @Test
    fun `iterator walks leaf to root`() {
        val id = DslId("root") + "mid" + "leaf"
        val list = id.toList()
        assertEquals(3, list.size)
        assertEquals("leaf", list[0].topElement)
        assertEquals("mid", list[1].topElement)
        assertEquals("root", list[2].topElement)
    }

    @Test
    fun `iterator next returns nodes in order`() {
        val id = DslId("a") + "b"
        val iter = id.iterator()
        assertTrue(iter.hasNext())
        assertEquals("b", iter.next().topElement)
        assertTrue(iter.hasNext())
        assertEquals("a", iter.next().topElement)
        assertFalse(iter.hasNext())
    }

    @Test
    fun `toString includes all objects`() {
        val id = DslId("x") + "y"
        val str = id.toString()
        assertTrue(str.contains("x"))
        assertTrue(str.contains("y"))
    }

    @Test
    fun `null obj is allowed`() {
        val id = DslId(null)
        assertEquals(1, id.size)
        assertNull(id.topElement)
    }

    @Test
    fun `root with no prevNode`() {
        val root = DslId("root")
        assertEquals(1, root.size)
    }
}
