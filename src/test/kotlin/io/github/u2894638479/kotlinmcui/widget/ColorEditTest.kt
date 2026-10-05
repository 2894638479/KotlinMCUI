package io.github.u2894638479.kotlinmcui.widget

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.Slider
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.HDouble
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.RDouble
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.math.abs
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class ColorEditTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = dslBackendProvider
        dslBackendProvider = { TestBackend }
    }

    @AfterEach
    fun tearDown() { dslBackendProvider = previousBackendProvider }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test",function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    private fun colorProp(initial: Color): Pair<StableRW<Color>, () -> Color> {
        var color = initial
        val prop = object : StableRW<Color> {
            override fun getValue() = color
            override fun setValue(value: Color) { color = value }
        }
        return prop to { color }
    }

    @Test
    fun `the square turns saturation and value into a colour`() {
        assertEquals(Color.RED,squareColor(0.0,1.0,0.0,1.0),"right edge, top: the pure hue")
        assertEquals(Color.WHITE,squareColor(0.0,0.0,0.0,1.0),"left edge, top: white")
        assertEquals(Color.BLACK,squareColor(0.0,1.0,1.0,1.0),"bottom: black whatever the saturation")
        assertEquals(127,squareColor(0.0,0.5,0.5,0.5).aInt,"the alpha passes through, quantised to a byte")
    }

    @Test
    fun `the triangle corners are the pure hue, white and black`() {
        assertEquals(Color.RED,triangleColor(0.0,0.5,0.0,1.0),"the top corner is the pure hue")
        assertEquals(Color.WHITE,triangleColor(0.0,0.0,1.0,1.0),"bottom-left is white")
        assertEquals(Color.BLACK,triangleColor(0.0,1.0,1.0,1.0),"bottom-right is black")
        assertNull(triangleColor(0.0,1.0,0.0,1.0),"the point above the right corner is outside the triangle")
        assertNotNull(triangleColor(0.0,0.5,1.0,1.0),"the middle of the bottom edge is still on the triangle")
        assertNull(triangleColor(0.0,0.5,1.05,1.0),"just below that edge is outside")
    }

    @Test
    fun `the triangle mixes the hue with white and black`() {
        // Halfway between the hue corner and the white one: red keeps its channel, the other two
        // are lifted equally, which is what the backend interpolates as well.
        val mixed = assertNotNull(triangleColor(0.0,0.25,0.5,1.0))
        assertTrue(mixed.rDouble > 0.9,"red survives")
        assertTrue(mixed.gDouble in 0.4..0.6 && mixed.bDouble in 0.4..0.6,"green and blue are lifted alike")
        assertEquals(mixed.gDouble,mixed.bDouble,0.01)
    }

    @Test
    fun `the marker of the triangle inverts the pick`() {
        for(sector in 0 until 6) for(x in 1 until 10) for(y in 1 until 10) {
            val pickX = x / 10.0
            val pickY = y / 10.0
            val color = triangleColor(sector / 6.0,pickX,pickY,1.0) ?: continue
            val (markerX,markerY) = triangleMarker(color)
            assertTrue(
                abs(markerX - pickX) < 0.02 && abs(markerY - pickY) < 0.02,
                "hue $sector picked at $pickX,$pickY came back at $markerX,$markerY"
            )
        }
    }

    @Test
    fun `hex text round-trips and rejects what it cannot read`() {
        val opaque = Color(255,128,0)
        assertEquals("#ff8000",formatColorHex(opaque))
        assertEquals(opaque,parseColorHex("#ff8000"))
        assertEquals(opaque,parseColorHex("ff8000"),"the hash is optional")
        assertEquals(Color(255,136,0),parseColorHex("#f80"),"three digits are expanded one at a time")
        assertEquals(Color(255,128,0,128),parseColorHex("#ff800080"))
        val transparent = Color(255,128,0,0)
        assertEquals("#ff800000",formatColorHex(transparent),"a transparent colour shows its alpha")
        assertEquals(transparent,parseColorHex(formatColorHex(transparent)))
        assertNull(parseColorHex("#ff800"),"five digits are not a colour")
        assertNull(parseColorHex("#gggggg"))
        assertNull(parseColorHex(""))
    }

    @Test
    fun `a colour slider edits the channel it was given`() {
        val (prop,read) = colorProp(Color(10,20,30,40))
        val screen = buildScreen {
            Slider(Modifier.size(100.px,20.px),Axis.Horizontal,RDouble,prop,id = "red") {}
        }
        val slider = screen.children.single()
        val rightEnd = Position(slider.rect.right - 1.px,slider.rect.top + slider.rect.height / 2)
        context(KeyModifiers(),rightEnd) { assertTrue(slider.mouseDown(MouseButton.LEFT),"a press on the track is taken") }
        assertEquals(Color(255,20,30,40),read(),"the right end is the top of the ramp, and only red moved")
    }

    @Test
    fun `the hue slider moves the hue and leaves the rest alone`() {
        val start = Color(10,20,30,40)
        val (prop,read) = colorProp(start)
        val screen = buildScreen {
            Slider(Modifier.size(100.px,20.px),Axis.Horizontal,HDouble,prop,id = "hue") {}
        }
        val slider = screen.children.single()
        val third = Position(slider.rect.left + slider.rect.width / 3,slider.rect.top + slider.rect.height / 2)
        context(KeyModifiers(),third) { assertTrue(slider.mouseDown(MouseButton.LEFT)) }
        val moved = read()
        assertEquals(1.0 / 3,moved.hDouble,0.05,"the hue follows the position")
        assertEquals(40,moved.aInt,"the alpha is untouched")
        assertEquals(start.sDouble,moved.sDouble,0.05,"so is the saturation")
        assertEquals(start.vDouble,moved.vDouble,0.05,"and the value")
    }

    @Test
    fun `pressing the square sets saturation and value`() {
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { Color.WHITE }
            prop = p
            Controls.ColorSquare(p,Modifier.size(100.px,100.px),"square")
        }
        val square = screen.children.single()
        val rightTop = Position(square.rect.right - 1.px,square.rect.top + 1.px)
        context(KeyModifiers(),rightTop) {
            assertTrue(square.mouseDown(MouseButton.LEFT),"a left press inside the square is taken")
        }
        val picked = assertNotNull(prop).value
        assertTrue(picked.sDouble > 0.9,"the right edge is full saturation")
        assertTrue(picked.vDouble > 0.9,"the top edge is full value")
        assertEquals(0.0,picked.hDouble,0.02,"the hue is the one the square was showing")

        val leftBottom = Position(square.rect.left + 1.px,square.rect.bottom - 1.px)
        context(KeyModifiers(),leftBottom) {
            assertTrue(square.mouseDown(MouseButton.LEFT))
        }
        val dark = assertNotNull(prop).value
        assertTrue(dark.vDouble < 0.05 && dark.sDouble < 0.05,"the bottom-left corner is black")

        val outside = Position(square.rect.right + 5.px,square.rect.top + 5.px)
        context(KeyModifiers(),outside) {
            assertFalse(square.mouseDown(MouseButton.LEFT),"a press outside the square is not taken")
        }
        context(KeyModifiers(),rightTop) {
            assertFalse(square.mouseDown(MouseButton.RIGHT),"other buttons are left to the delegate")
        }
    }

    @Test
    fun `a point outside a palette is held at its nearest edge`() {
        // The square is the whole rectangle, so its nearest point is a plain coerce.
        assertEquals(0.0 to 0.0,nearestOnUnitSquare(-0.5,-0.5))
        assertEquals(1.0 to 1.0,nearestOnUnitSquare(1.5,1.5))

        // The triangle projects onto its closest edge - the corners fall out of the same projection.
        val onEdge = nearestOnUnitTriangle(1.0,0.0)
        assertEquals(0.6,onEdge.first,1e-6,"the closest point of the right edge, not a weight renorm")
        assertEquals(0.2,onEdge.second,1e-6)
        assertEquals(0.5 to 0.0,nearestOnUnitTriangle(0.5,-0.5),"above the top corner")
        assertEquals(0.0 to 1.0,nearestOnUnitTriangle(-0.5,1.0),"left of the bottom-left corner")
        assertEquals(1.0 to 1.0,nearestOnUnitTriangle(1.5,1.4),"below the bottom-right corner")

        // A point on the triangle is its own nearest point, and off it the colour functions refuse.
        assertEquals(0.5 to 1.0,nearestOnUnitTriangle(0.5,1.0))
        assertNull(triangleColor(0.0,0.5,-0.5,1.0),"off the triangle there is no colour")
        assertNull(triangleColor(0.0,0.5,1.4,1.0))
    }

    @Test
    fun `a press outside the palette must not capture the release`() {
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { Color.WHITE }
            prop = p
            Controls.ColorSquare(p,Modifier.size(50.px,50.px),"square")
        }
        val square = screen.children.single()
        val start = assertNotNull(prop).value
        val outside = Position(square.rect.right + 5.px,square.rect.top + 5.px)
        context(KeyModifiers(),outside) {
            assertFalse(square.mouseDown(MouseButton.LEFT),"a press outside the palette is not taken")
            // Returning true here is what stops the event on its way down the tree, so a release
            // that does not belong to the palette must return false and keep travelling.
            assertFalse(square.mouseUp(MouseButton.LEFT),"so the release must keep travelling")
        }
        assertEquals(start,assertNotNull(prop).value,"and none of it may change the colour")
    }

    @Test
    fun `the demo colour is the one the picker starts from`() {
        val start = Color.ofHSV(0.08,1.0,1.0)
        assertEquals(255,start.rInt,"hue 0.08 is an orange, not a yellow")
        assertEquals(122,start.gInt)
        assertEquals(0,start.bInt)
        assertEquals("#ff7a00",formatColorHex(start))
    }

    @Test
    fun `the hue triangle picks the mixture of its three corners`() {
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { Color.WHITE }
            prop = p
            Controls.ColorHueTriangle(p,Modifier.size(60.px,60.px),"hue-triangle")
        }
        val triangle = screen.children.single()
        fun press(x: Measure,y: Measure) = context(KeyModifiers(),Position(x,y)) {
            triangle.mouseDown(MouseButton.LEFT)
        }
        press(triangle.rect.left + triangle.rect.width / 2,triangle.rect.top)
        assertEquals(Color.RED,assertNotNull(prop).value,"the top corner is the red hue")
        press(triangle.rect.left,triangle.rect.bottom)
        assertEquals(Color.GREEN,assertNotNull(prop).value,"the bottom-left corner is green")
        press(triangle.rect.right,triangle.rect.bottom)
        assertEquals(Color.BLUE,assertNotNull(prop).value,"and the bottom-right is blue")
    }

    @Test
    fun `the hue triangle marker normalises the colour to its position`() {
        assertEquals(0.5 to 0.0,hueTriangleMarker(Color.RED))
        assertEquals(0.0 to 1.0,hueTriangleMarker(Color.GREEN))
        assertEquals(1.0 to 1.0,hueTriangleMarker(Color.BLUE))
        // A mixture scaled by its value is the same point, so white sits in the middle.
        val white = hueTriangleMarker(Color.WHITE)
        assertEquals(0.5,white.first,1e-9)
        assertEquals(2.0 / 3,white.second,1e-9)
        assertEquals(0.5 to 2.0 / 3,hueTriangleMarker(Color.BLACK),"black has no position; the centre is nearest")
    }

    @Test
    fun `a press beside the hue bar is not taken`() {
        val start = Color.ofHSV(0.08,1.0,1.0)
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { start }
            prop = p
            Controls.ColorHue(p,Modifier.size(20.px,80.px),Axis.Vertical,"hue")
        }
        val bar = screen.children.single()
        // The same height as the bar, but well to its left: outside the widget altogether.
        val beside = Position(bar.rect.left - 30.px,bar.rect.top + bar.rect.height / 2)
        context(KeyModifiers(),beside) {
            assertFalse(bar.mouseDown(MouseButton.LEFT),"only a press on the bar may move the hue")
        }
        assertEquals(start,assertNotNull(prop).value,"and it must not have changed the colour")
    }

    @Test
    fun `a vertical hue bar moves the hue along it`() {
        val start = Color.ofHSV(0.08,1.0,1.0)
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { start }
            prop = p
            Controls.ColorHue(p,Modifier.size(20.px,80.px),Axis.Vertical,"hue")
        }
        val bar = screen.children.single()
        val quarter = Position(bar.rect.left + bar.rect.width / 2,bar.rect.top + bar.rect.height / 4)
        context(KeyModifiers(),quarter) { assertTrue(bar.mouseDown(MouseButton.LEFT)) }
        assertEquals(0.25,assertNotNull(prop).value.hDouble,0.02,"the hue follows the height on a vertical bar")
        // Saturation, value and alpha stay where they were.
        assertEquals(start.sDouble,assertNotNull(prop).value.sDouble,0.05)
        assertEquals(start.vDouble,assertNotNull(prop).value.vDouble,0.05)
        assertEquals(start.aInt,assertNotNull(prop).value.aInt)
    }

    @Test
    fun `a press above a horizontal hue bar is not taken`() {
        val start = Color.ofHSV(0.08,1.0,1.0)
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { start }
            prop = p
            Controls.ColorHue(p,Modifier.size(80.px,20.px),Axis.Horizontal,"hue")
        }
        val bar = screen.children.single()
        val above = Position(bar.rect.left + bar.rect.width / 2,bar.rect.top - 30.px)
        context(KeyModifiers(),above) {
            assertFalse(bar.mouseDown(MouseButton.LEFT),"the bar is not the whole column above it")
        }
        assertEquals(start,assertNotNull(prop).value,"and it must not have changed the colour")
    }

    @Test
    fun `pressing the hue bar moves the hue and leaves the rest alone`() {
        val start = Color(10,20,30,40)
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { start }
            prop = p
            Controls.ColorHue(p,Modifier.size(60.px,20.px),Axis.Horizontal,"hue")
        }
        val bar = screen.children.single()
        val third = Position(bar.rect.left + bar.rect.width / 3,bar.rect.top + bar.rect.height / 2)
        context(KeyModifiers(),third) { assertTrue(bar.mouseDown(MouseButton.LEFT)) }
        val moved = assertNotNull(prop).value
        assertEquals(1.0 / 3,moved.hDouble,0.02,"the hue follows the position")
        assertEquals(40,moved.aInt,"the alpha is untouched")
        assertEquals(start.sDouble,moved.sDouble,0.05,"so is the saturation")
        assertEquals(start.vDouble,moved.vDouble,0.05,"and the value")
    }

    @Test
    fun `the composite renders both pickers without touching the colour`() {
        val start = Color(20,140,200,255)
        var prop: StableRW<Color>? = null
        val screen = buildScreen {
            val p = local { start }
            prop = p
            Controls.ColorEdit(p,ColorPicker.Triangle,"edit")
        }
        TestBackend.renderFrame(screen)
        // The test backend cannot draw a triangle, so the editor falls back to the square rather
        // than offering an invisible area - and none of its parts may write the colour back.
        assertEquals(start,assertNotNull(prop).value)
        assertTrue(screen.children.isNotEmpty())
    }
}
