package io.github.u2894638479.kotlinmcui.math

import io.github.u2894638479.kotlinmcui.prop.*
import kotlin.math.abs
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

    // ── Keyboard stepping (uint8-only) ───────────────────────────────────────

    @Test
    fun `step RGBA by one byte, clamped at the ends`() {
        val c = Color(254u, 1u, 0u, 128u)
        assertEquals(Color(255u, 1u, 0u, 128u), RDouble.stepUp(c))
        assertEquals(Color(253u, 1u, 0u, 128u), RDouble.stepDown(c))
        assertEquals(Color(254u, 2u, 0u, 128u), GDouble.stepUp(c))
        assertEquals(Color(254u, 0u, 0u, 128u), GDouble.stepDown(c))
        assertEquals(Color(254u, 1u, 1u, 128u), BDouble.stepUp(c))
        assertEquals(Color(254u, 1u, 0u, 128u), BDouble.stepDown(c)) // b already 0: clamped
        assertEquals(Color(254u, 1u, 0u, 129u), ADouble.stepUp(c))
        assertEquals(Color(254u, 1u, 0u, 127u), ADouble.stepDown(c))
        // boundaries never wrap
        assertEquals(Color(255, 255, 255), RDouble.stepUp(Color.WHITE))
        assertEquals(Color.BLACK, RDouble.stepDown(Color.BLACK))
        assertEquals(Color(255, 255, 255), ADouble.stepUp(Color.WHITE))
        assertEquals(Color.TRANSPARENT_BLACK, ADouble.stepDown(Color.TRANSPARENT_BLACK))
    }

    @Test
    fun `step value moves the max byte together`() {
        // unique max: only it moves
        assertEquals(Color(201, 100, 50), VDouble.stepUp(Color(200, 100, 50)))
        assertEquals(Color(199, 100, 50), VDouble.stepDown(Color(200, 100, 50)))
        // tied max: all tied channels move, keeping the colour axis
        assertEquals(Color(201, 201, 50), VDouble.stepUp(Color(200, 200, 50)))
        assertEquals(Color(128, 128, 128), VDouble.stepUp(Color(127, 127, 127)))
        assertEquals(Color(254, 254, 254), VDouble.stepDown(Color(255, 255, 255)))
    }

    @Test
    fun `step saturation moves the min byte together, value stays fixed`() {
        assertEquals(Color(200, 99, 99), SDouble.stepUp(Color(200, 100, 100)))
        assertEquals(Color(200, 101, 101), SDouble.stepDown(Color(200, 100, 100)))
        // both max and min unchanged value-wise after a saturation step
        assertEquals(200, SDouble.stepUp(Color(200, 100, 100)).maxByte)
        assertEquals(200, SDouble.stepDown(Color(200, 100, 100)).maxByte)
        // fully saturated / achromatic clamp and break
        assertEquals(Color(200, 100, 0), SDouble.stepUp(Color(200, 100, 0)))
        assertEquals(Color(127, 128, 128), SDouble.stepUp(Color(128, 128, 128)))
        assertEquals(Color(128, 128, 128), SDouble.stepDown(Color(128, 128, 128)))
    }

    @Test
    fun `step hue chooses the closest different hue`() {
        // red (hue 0): the nearest representable hue above is one green tick
        assertEquals(Color(255, 1, 0), HDouble.stepUp(Color.RED))
        // step either direction always yields a distinct colour
        val mid = Color(200, 100, 0) // hue 30°
        val up = HDouble.stepUp(mid)
        val down = HDouble.stepDown(mid)
        assertNotEquals(mid, up)
        assertNotEquals(mid, down)
        assertTrue(up.hDouble > mid.hDouble)
        assertTrue(down.hDouble < mid.hDouble)
    }

    @Test
    fun `step hue wraps around the circular wheel`() {
        // stepping down from red (hue 0) wraps to just below 360°
        val down = HDouble.stepDown(Color.RED)
        assertNotEquals(Color.RED, down)
        assertTrue(down.hDouble > 0.9) // hue just below 360°/1.0
        // stepping up from just below 360° wraps to just above 0°
        val top = Color(200, 0, 1) // hue ≈ 359.4°
        val up = HDouble.stepUp(top)
        assertTrue(up.hDouble < 0.1)
    }

    @Test
    fun `step hue keeps S and V exactly fixed`() {
        for (color in listOf(Color(200, 100, 0), Color(200, 200, 50), Color(120, 30, 30), Color(200, 199, 50))) {
            val up = HDouble.stepUp(color)
            val down = HDouble.stepDown(color)
            for (stepped in listOf(up, down)) {
                assertEquals(color.maxByte, stepped.maxByte, "V drifted for $color")
                assertEquals(color.sDouble, stepped.sDouble, "S drifted for $color")
            }
        }
        // achromatic colours have no hue to step
        assertEquals(Color(128, 128, 128), HDouble.stepUp(Color(128, 128, 128)))
        assertEquals(Color(128, 128, 128), HDouble.stepDown(Color(128, 128, 128)))
    }

    @Test
    fun `step value keeps hue untouched when the max is tied`() {
        // symmetric max stays symmetric, so hue is preserved exactly
        val c = Color(200, 200, 50)
        val up = VDouble.stepUp(c)
        val down = VDouble.stepDown(c)
        assertEquals(c.hDouble, up.hDouble)
        assertEquals(c.hDouble, down.hDouble)
        assertEquals(Color(201, 201, 50), up)
        assertEquals(Color(199, 199, 50), down)
        // S cannot be preserved exactly: max±1 with min fixed needs min' = min±min/max
        assertNotEquals(c.sDouble, up.sDouble)
        assertTrue(abs(up.sDouble - c.sDouble) < 0.01)
    }

    @Test
    fun `step saturation keeps V fixed`() {
        for (color in listOf(Color(200, 100, 100), Color(200, 150, 100), Color(128, 128, 128), Color(255, 128, 0))) {
            assertEquals(color.maxByte, SDouble.stepUp(color).maxByte)
            assertEquals(color.maxByte, SDouble.stepDown(color).maxByte)
        }
    }

    @Test
    fun `mixedWith - channel-wise multiply, WHITE is identity`() {
        val c = Color(100, 150, 200, 80)
        assertEquals(c, Color.WHITE.mixedWith(c))
        assertEquals(c, c.mixedWith(Color.WHITE))
        // scaled by half
        assertEquals(Color(50, 75, 100, 40), c.mixedWith(Color(128, 128, 128, 128)))
    }

    @Test
    fun `screenWith - channel-wise screen, alpha included`() {
        // BLACK has zero rgb: rgb is kept, while alpha screens to opaque
        assertEquals(Color(100, 150, 200, 255), Color.BLACK.screenWith(Color(100, 150, 200)))
        assertEquals(Color.WHITE, Color.WHITE.screenWith(Color(100, 150, 200)))
        // 128 screen 128 = 255 - 127 * 127 / 255 = 192, on every channel including alpha
        assertEquals(Color(192, 192, 192, 192), Color(128, 128, 128, 128).screenWith(Color(128, 128, 128, 128)))
    }

    @Test
    fun `averageWith - channel-wise midpoint, alpha included`() {
        assertEquals(Color(127, 127, 127, 255), Color.BLACK.averageWith(Color.WHITE))
        assertEquals(Color(150, 100, 50, 255), Color(100, 200, 0, 255).averageWith(Color(200, 0, 100, 255)))
        assertEquals(Color(0, 0, 0, 150), Color(0, 0, 0, 100).averageWith(Color(0, 0, 0, 200)))
    }

    @Test
    fun `over - source-over alpha compositing`() {
        // an opaque source covers the destination
        assertEquals(Color.RED, Color.RED.over(Color.BLUE))
        // a transparent source leaves the destination untouched
        assertEquals(Color.BLUE, Color.TRANSPARENT_BLACK.over(Color.BLUE))
        // two transparent colours stay transparent
        assertEquals(Color.TRANSPARENT_BLACK, Color.TRANSPARENT_BLACK.over(Color.TRANSPARENT_BLACK))
        // 50% white over opaque blue = light blue
        assertEquals(Color(128, 128, 255, 255), Color.WHITE.change(a = 128).over(Color.BLUE))
        // 50% white over opaque black = mid gray
        assertEquals(Color(128, 128, 128, 255), Color.WHITE.change(a = 128).over(Color.BLACK))
        // over a transparent destination the source keeps its own alpha
        assertEquals(Color(255, 255, 255, 128), Color.WHITE.change(a = 128).over(Color.TRANSPARENT_BLACK))
    }

    private val Color.maxByte: Int get() = maxOf(rInt, gInt, bInt)
}
