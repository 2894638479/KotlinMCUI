package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.math.px
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * `TestRoot.setFocus` is the programmatic way to move the focus: how a screen puts the
 * caret in a field when it opens, or hands it to the next control without a mouse.
 */
class DslFocusTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() {
        TestRoot.setFocus(null)
        TestRuntime.restore(previousBackendProvider)
    }

    private fun buildScreen(function: DslFunction): DslScreen {
        val screen = TestBackend.create("test", function).dslScreen
        TestBackend.renderFrame(screen)
        return screen
    }

    private fun editable(initial: String): StableRW<String> {
        var text = initial
        return object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
    }

    @Test
    fun `a code request focuses the component and it reports being focused`() {
        val screen = buildScreen { EditableText(stringProp = editable("abc"), id = "box") }
        val box = screen.children[0]
        assertFalse(box.isFocused, "nothing is focused before the request")

        // The source defaults to CODE, which is what distinguishes this from a mouse or
        // keyboard move for anything listening on globalFocusChanged.
        TestRoot.setFocus(box.identity)

        assertEquals(box.identity, TestRoot.focused)
        assertTrue(box.isFocused, "the target reports being focused")
    }

    @Test
    fun `a null request clears the focus`() {
        val screen = buildScreen { EditableText(stringProp = editable("abc"), id = "box") }
        val box = screen.children[0]
        TestRoot.setFocus(box.identity)

        TestRoot.setFocus(null)

        assertNull(TestRoot.focused)
        assertFalse(box.isFocused)
    }

    @Test
    fun `a target that cannot take focus is still recorded but reports itself unfocused`() {
        val screen = buildScreen { Spacer(Modifier.size(10.px, 10.px), "plain") }
        val plain = screen.children[0]
        assertFalse(plain.focusable, "the fixture must not be focusable")

        // The setter is the raw primitive and does not filter: requesting a plain area records
        // it, while isFocused still answers by the component's own focusable flag.
        TestRoot.setFocus(plain.identity)

        assertEquals(plain.identity, TestRoot.focused)
        assertFalse(plain.isFocused)
    }
}
