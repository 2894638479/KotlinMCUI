package io.github.u2894638479.kotlinmcui.math

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorPropsTest {
    private val orange = Color(220,120,40,255)

    @Test fun `every channel is named`() {
        assertEquals("red", RDouble.name)
        assertEquals("green", GDouble.name)
        assertEquals("blue", BDouble.name)
        assertEquals("alpha", ADouble.name)
        assertEquals("hue", HDouble.name)
        assertEquals("saturation", SDouble.name)
        assertEquals("value", VDouble.name)
        assertEquals("rgba", RGBAInt.name)
    }

    @Test fun `byte channels read their own byte`() {
        // Reading through the byte instead of scaling the 0..1 double: 220*255/255 truncates to 219.
        assertEquals("220", RDouble.toString(orange))
        assertEquals("120", GInt.toString(orange))
        assertEquals("40", BFloat.toString(orange))
        assertEquals("255", ADouble.toString(orange))
    }

    @Test fun `hsv channels read with their unit`() {
        assertEquals("26°", HDouble.toString(orange))
        assertEquals("81%", SDouble.toString(orange))
        assertEquals("86%", VDouble.toString(orange))
    }

    @Test fun `packed channels read as hexadecimal`() {
        assertEquals("#dc7828ff", RGBAInt.toString(orange))
    }

    @Test fun `stepping a double channel moves one byte`() {
        assertEquals(Color(221,120,40,255), RDouble.stepUp(orange))
        assertEquals(Color(221,120,40,255), VDouble.stepUp(orange))
    }
}
