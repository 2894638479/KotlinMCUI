package io.github.u2894638479.kotlinmcui.math

import kotlin.test.*

class ColorTest {

    @Test
    fun `constructor from UByte`() {
        val c = Color(255u, 128u, 64u, 32u)
        assertEquals(255u, c.r)
        assertEquals(128u, c.g)
        assertEquals(64u, c.b)
        assertEquals(32u, c.a)
    }

    @Test
    fun `constructor from Int clamps values`() {
        val c = Color(300, -10, 128, 255)
        assertEquals(255, c.rInt)
        assertEquals(0, c.gInt)
        assertEquals(128, c.bInt)
        assertEquals(255, c.aInt)
    }

    @Test
    fun `constructor from Float`() {
        val c = Color(1f, 0.5f, 0.25f, 1f)
        assertEquals(255, c.rInt)
        assertEquals(127, c.gInt)
        assertEquals(63, c.bInt)
        assertEquals(255, c.aInt)
    }

    @Test
    fun `constructor from Double`() {
        val c = Color(1.0, 0.5, 0.0, 1.0)
        assertEquals(255, c.rInt)
        assertEquals(127, c.gInt)
        assertEquals(0, c.bInt)
        assertEquals(255, c.aInt)
    }

    @Test
    fun `channel access as Float`() {
        val c = Color(255u, 128u, 0u, 255u)
        assertEquals(1.0f, c.rFloat)
        assertEquals(0.5f, c.gFloat, 0.01f)
        assertEquals(0.0f, c.bFloat)
        assertEquals(1.0f, c.aFloat)
    }

    @Test
    fun `channel access as Double`() {
        val c = Color(255u, 0u, 0u, 255u)
        assertEquals(1.0, c.rDouble)
        assertEquals(0.0, c.gDouble)
    }

    @Test
    fun `companion WHITE BLACK RED GREEN BLUE`() {
        assertEquals(255u, Color.WHITE.r)
        assertEquals(255u, Color.WHITE.g)
        assertEquals(255u, Color.WHITE.b)
        assertEquals(255u, Color.WHITE.a)
        assertEquals(0u, Color.BLACK.r)
        assertEquals(0u, Color.BLACK.g)
        assertEquals(0u, Color.BLACK.b)
        assertEquals(255u, Color.BLACK.a)
        assertEquals(255u, Color.RED.r)
        assertEquals(0u, Color.RED.g)
        assertEquals(0u, Color.RED.b)
    }

    @Test
    fun `packed format argbInt`() {
        val c = Color(255u, 128u, 64u, 32u)
        val argb = c.argbInt
        assertEquals(c, Color.ofARGB(argb))
    }

    @Test
    fun `packed format abgrInt`() {
        val c = Color(255u, 128u, 64u, 32u)
        val abgr = c.abgrInt
        assertEquals(c, Color.ofABGR(abgr))
    }

    @Test
    fun `ofRGBA roundtrip`() {
        val c = Color(255u, 128u, 64u, 32u)
        assertEquals(c, Color.ofRGBA(c.rgbaInt))
    }

    @Test
    fun `HSV conversion - red`() {
        val c = Color(255u, 0u, 0u, 255u)
        assertEquals(0.0f, c.hFloat, 0.01f)
        assertEquals(1.0f, c.sFloat, 0.01f)
        assertEquals(1.0f, c.vFloat, 0.01f)
    }

    @Test
    fun `HSV conversion - white`() {
        val c = Color(255u, 255u, 255u, 255u)
        assertEquals(0.0f, c.sFloat, 0.01f)
        assertEquals(1.0f, c.vFloat, 0.01f)
    }

    @Test
    fun `HSV conversion - black`() {
        val c = Color(0u, 0u, 0u, 255u)
        assertEquals(0.0f, c.sFloat, 0.01f)
        assertEquals(0.0f, c.vFloat, 0.01f)
    }

    @Test
    fun `ofHSV roundtrip for red`() {
        val c = Color.ofHSV(0f, 1f, 1f)
        assertEquals(255, c.rInt)
        assertEquals(0, c.gInt)
        assertEquals(0, c.bInt)
    }

    @Test
    fun `change - immutable modification of single channel`() {
        val c = Color(255u, 0u, 0u, 128u)
        val c2 = c.change(g = 255u)
        assertEquals(255u, c2.r)
        assertEquals(255u, c2.g)
        assertEquals(0u, c2.b)
        assertEquals(128u, c2.a)
        // original unchanged
        assertEquals(0u, c.g)
    }

    @Test
    fun `changeHSV - modify hue to green`() {
        val red = Color.ofHSV(0f, 1f, 1f)
        val green = red.changeHSV(h = 1f / 3f) // 120 degrees = green
        assertEquals(0, green.rInt)
        assertEquals(255, green.gInt)
        assertEquals(0, green.bInt)
    }

    @Test
    fun `arithmetic - plus adds channels with clamping`() {
        val a = Color(100, 50, 25, 200)
        val b = Color(100, 50, 25, 55)
        val result = a + b
        assertEquals(200, result.rInt)
        assertEquals(100, result.gInt)
        assertEquals(50, result.bInt)
        assertEquals(255, result.aInt)
    }

    @Test
    fun `arithmetic - minus subtracts channels`() {
        val a = Color(200, 100, 50, 200)
        val b = Color(100, 50, 25, 100)
        val result = a - b
        assertEquals(100, result.rInt)
        assertEquals(50, result.gInt)
        assertEquals(25, result.bInt)
        assertEquals(100, result.aInt)
    }

    @Test
    fun `times with Double scales channels`() {
        val c = Color(100, 200, 50, 128)
        val result = c * 0.5
        assertEquals(50, result.rInt)
        assertEquals(100, result.gInt)
        assertEquals(25, result.bInt)
        assertEquals(64, result.aInt)
    }

    @Test
    fun `TRANSPARENT_WHITE has alpha 0`() {
        assertEquals(0u, Color.TRANSPARENT_WHITE.a)
        assertEquals(255u, Color.TRANSPARENT_WHITE.r)
    }

    @Test
    fun `TRANSPARENT_BLACK has alpha 0`() {
        assertEquals(0u, Color.TRANSPARENT_BLACK.a)
        assertEquals(0u, Color.TRANSPARENT_BLACK.r)
    }

    @Test
    fun `hexStringARGB format`() {
        val c = Color(255u, 128u, 64u, 32u)
        // ARGB = a(32)=0x20, r(255)=0xFF, g(128)=0x80, b(64)=0x40
        assertEquals("20ff8040", c.hexStringARGB)
    }
}
