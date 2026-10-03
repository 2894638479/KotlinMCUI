package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.component.FocusSource
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslEditBoxTest {

    @BeforeEach
    fun setUp() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = { TestBackend } }

    @AfterEach
    fun tearDown() { io.github.u2894638479.kotlinmcui.backend.dslBackendProvider = null }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    private fun editable(initial: String): Pair<StableRW<String>, () -> String> {
        var text = initial
        val prop = object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
        return prop to { text }
    }

    @Test
    fun `shift right at the end of the text keeps the caret inside the text`() {
        val (prop, text) = editable("abc")
        val screen = buildScreen { EditableText(stringProp = prop, id = "box") }
        val box = screen.children[0]
        TopComponent.setFocus(box.identity, FocusSource.KEYBOARD)
        assertNotNull(box.preeditArea, "a focused edit box reports a caret")

        // Moving right from the last position used to push the cursor one past the
        // end, which made the caret vanish (and took the IME area with it) until the
        // next key event happened to resynchronise the model.
        repeat(2) { context(KeyModifiers(shift = true)) { box.keyDown(Key.RIGHT, 0) } }

        assertNotNull(box.preeditArea, "shift+right at the end must not move the caret out of range")
        assertEquals("abc", text())
    }

    @Test
    fun `shift right selects towards the end without consuming text`() {
        val (prop, text) = editable("abcd")
        val screen = buildScreen { EditableText(stringProp = prop, id = "box") }
        val box = screen.children[0]
        TopComponent.setFocus(box.identity, FocusSource.KEYBOARD)

        // Caret starts at the end of the text, so this is the boundary case above
        // followed by a backwards extension: the selection must stay well defined.
        context(KeyModifiers(shift = true)) { box.keyDown(Key.RIGHT, 0) }
        context(KeyModifiers(shift = true)) { box.keyDown(Key.LEFT, 0) }

        assertNotNull(box.preeditArea)
        assertEquals("abcd", text())
    }
}
