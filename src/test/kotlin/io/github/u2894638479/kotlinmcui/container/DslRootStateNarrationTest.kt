package io.github.u2894638479.kotlinmcui.container

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * The narration rule, which nothing else observes.
 *
 * `TestBackend.narrate` is a no-op and no other test reads `keyboardNarration`/`mouseNarration`, so
 * this rule can be broken while the rest of the suite stays green - measured, not assumed: announcing
 * unconditionally instead of only on a change left all the other cases passing. The announcement is
 * therefore asserted here by counting the callback, which is the only place it is visible.
 *
 * Falsifiable: take the `if(text != ...)` layer out of `DslRootState.noteKeyboardNarration` /
 * `noteMouseNarration` (keeping the `onNarrate` call) and the first two cases below go red.
 */
class DslRootStateNarrationTest {
    private fun state(announced: MutableList<String>) = DslRootState(
        onFocusChanged = { _, _ -> },
        onHoverChanged = {},
        onNarrate = { announced += it }
    )

    @Test
    fun `the same sentence is not announced twice`() {
        val announced = mutableListOf<String>()
        val state = state(announced)

        state.noteKeyboardNarration("Button")
        state.noteKeyboardNarration("Button")
        state.noteKeyboardNarration("Slider")
        state.noteKeyboardNarration(null)

        assertEquals(listOf("Button", "Slider"), announced, "only a change is news")
        assertNull(state.keyboardNarration, "null clears the record")
    }

    @Test
    fun `the pointer path follows the same rule`() {
        val announced = mutableListOf<String>()
        val state = state(announced)

        state.noteMouseNarration("Hovered")
        state.noteMouseNarration("Hovered")
        state.noteMouseNarration(null)
        state.noteMouseNarration(null)

        assertEquals(listOf("Hovered"), announced)
        assertNull(state.mouseNarration)
    }

    @Test
    fun `the two paths keep their own last text`() {
        val announced = mutableListOf<String>()
        val state = state(announced)

        state.noteKeyboardNarration("same")
        state.noteMouseNarration("same")

        assertEquals("same", state.keyboardNarration)
        assertEquals("same", state.mouseNarration)
        assertEquals(listOf("same", "same"), announced, "one path's text does not silence the other")
    }
}
