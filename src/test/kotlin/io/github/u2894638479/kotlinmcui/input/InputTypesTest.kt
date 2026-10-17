package io.github.u2894638479.kotlinmcui.input

import io.github.u2894638479.kotlinmcui.backend.DslRootInput
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import kotlin.test.*

class InputTypesTest {

    @Test
    fun `custom keys must use the extension range`() {
        assertEquals(Key.CUSTOM_RANGE_START, Key.custom(Key.CUSTOM_RANGE_START).code)
        assertFailsWith<IllegalArgumentException> { Key.custom(1) }
    }

    @Test
    fun `standard key codes stay in their documented ranges`() {
        assertTrue(Key.ENTER.code in 1..19)
        assertTrue(Key.LEFT_SHIFT.code in 20..29)
        assertTrue(Key.A.code in 30..55)
        assertTrue(Key.Z.code in 30..55)
        assertTrue(Key.N0.code in 60..69)
        assertTrue(Key.F25.code in 70..94)
    }

    @Test
    fun `mouse buttons map to their platform index`() {
        assertEquals(MouseButton.LEFT, MouseButton.from(0))
        assertEquals(MouseButton.MIDDLE, MouseButton.from(2))
        assertEquals(MouseButton.BUTTON7, MouseButton.from(6))
        assertFailsWith<IllegalStateException> { MouseButton.from(99) }
    }

    @Test
    fun `isMouseDown accepts a MouseButton`() {
        var lastIndex = -1
        val input = object : DslRootInput {
            override fun isKeyDown(key: Key) = false
            override fun isMouseDown(mouse: Int): Boolean {
                lastIndex = mouse
                return true
            }
            override val mouse = Position(0.px, 0.px)
            override fun setPreeditArea(rect: Rect) {}
        }
        assertTrue(input.isMouseDown(MouseButton.RIGHT))
        assertEquals(1, lastIndex, "the typed overload must forward the platform index")
    }
}
