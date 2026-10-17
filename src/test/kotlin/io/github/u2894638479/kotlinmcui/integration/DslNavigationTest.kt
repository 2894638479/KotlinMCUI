package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Keyboard traversal runs from the root, which is not a navigation node: it has no identity and
 * cannot take focus. TAB, HOME and END walk a flat view of the tree, so that view must contain the
 * tree and not the node the walk started from.
 */
class DslNavigationTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslPlatform)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() {
        TestRoot.setFocus(null)
        // Move the page out of the root again: `show` mounts it, and a leftover child would be
        // navigated into by every later case in the same JVM.
        TestBackend.resetPreview()
        TestRuntime.restore(previousBackendProvider)
    }

    /**
     * Mount a page the way the framework does and run one frame, so the root can see it and its
     * components have their rects - the grid views group children by rect, and the traversal only
     * reaches what the root holds.
     */
    private fun buildScreen(function: DslFunction): DslScreen {
        val holder = TestBackend.create("test", function)
        holder.show()
        TestBackend.renderFrame(holder.dslScreen)
        return holder.dslScreen
    }

    private fun editable(initial: String): StableRW<String> {
        var text = initial
        return object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
    }

    private fun oneControl(): DslScreen = buildScreen { EditableText(stringProp = editable("abc"), id = "box") }

    @Test
    fun `HOME focuses the first control`() {
        val screen = oneControl()
        context(KeyModifiers()) { TestRoot.keyDown(Key.HOME, 0) }
        assertEquals(screen.children[0].identity, TestRoot.focused)
    }

    @Test
    fun `TAB focuses the first control when nothing is focused yet`() {
        val screen = oneControl()
        context(KeyModifiers()) { TestRoot.keyDown(Key.TAB, 0) }
        assertEquals(screen.children[0].identity, TestRoot.focused)
    }

    @Test
    fun `END focuses a control`() {
        oneControl()
        context(KeyModifiers()) { TestRoot.keyDown(Key.END, 0) }
        assertNotNull(TestRoot.focused, "END must land on a control")
    }

    @Test
    fun `an arrow key reaches the grid view`() {
        oneControl()
        context(KeyModifiers()) { TestRoot.keyDown(Key.DOWN, 0) }
        assertNotNull(TestRoot.focused, "DOWN must land on a control")
    }
}
