package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * The press is the input path that moves the focus, and nothing else observes it.
 *
 * The rest of the suite either injects focus with `TestRoot.setFocus(...)` or drives the
 * keyboard, so the acquisition in `TestRoot.mouseDown` can be deleted while every other case
 * stays green - measured: removing the `acquireFocusFromInput` call from it left all 446 cases
 * passing. This case exists to make that deletion fail.
 *
 * Falsifiable: delete the `acquireFocusFromInput(::setFocus)` line from `TestRoot.mouseDown`
 * and the last assertion below fails, because nothing is focused after the press.
 */
class DslMouseFocusTest {
    /** Process-wide state: put back whatever provider was published before the case. */
    private var previous: (() -> DslPlatform)? = null

    @BeforeEach
    fun setUp() {
        previous = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() {
        TestRoot.setFocus(null)
        // Move the page out of the root again: `show` mounts it, and a leftover child would be
        // navigated into by every later case in the same JVM.
        TestBackend.resetPreview()
        TestRuntime.restore(previous)
    }

    private fun editable(initial: String): StableRW<String> {
        var text = initial
        return object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
    }

    @Test
    fun `a press moves the focus to what is under the pointer`() {
        val holder = TestBackend.create("mouse focus") { EditableText(stringProp = editable("abc"), id = "box") }
        holder.show()
        // One frame, so the control has its rect and the root holds the page: the hit test walks
        // the root's children, not the screen.
        TestBackend.renderFrame(holder.dslScreen)
        val target = holder.dslScreen.children[0]
        val rect = target.rect
        val at = Position(rect.left + 1.px, rect.top + 1.px)
        assertNull(TestRoot.focused, "the press is what focuses, so nothing is focused yet")

        context(KeyModifiers(), at) { TestRoot.mouseDown(MouseButton.LEFT) }

        assertEquals(target.identity, TestRoot.focused, "the press must move the focus to it")
    }
}
