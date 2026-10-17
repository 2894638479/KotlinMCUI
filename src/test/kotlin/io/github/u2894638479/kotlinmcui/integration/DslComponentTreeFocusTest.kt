package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslPlatform
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHovered
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * A component answers about focus from the tree it belongs to, not from the process-wide root.
 *
 * Until a screen's store carries its root's state, every component answered from `TestRoot`, so
 * two trees could not be told apart at all. This is the case that makes the difference observable:
 * each screen gets its own state and each component reports its own focus.
 */
class DslComponentTreeFocusTest {
    /** Process-wide state: put back whatever provider was published before the case. */
    private var previous: (() -> DslPlatform)? = null

    @BeforeEach
    fun setUp() {
        previous = TestRuntime.publish()
    }

    @AfterEach
    fun tearDown() {
        TestRoot.setFocus(null)
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
    fun `each component reads the focus of its own tree`() {
        val holderA = TestBackend.create("tree a") { EditableText(stringProp = editable("a"), id = "box") }
        val holderB = TestBackend.create("tree b") { EditableText(stringProp = editable("b"), id = "box") }
        // One frame each, so both trees are built and their components have rects.
        TestBackend.renderFrame(holderA.dslScreen)
        TestBackend.renderFrame(holderB.dslScreen)
        val a = holderA.dslScreen.children[0]
        val b = holderB.dslScreen.children[0]

        val stateA = DslRootState(onFocusChanged = { _, _ -> }, onHoverChanged = {}, onNarrate = {})
        val stateB = DslRootState(onFocusChanged = { _, _ -> }, onHoverChanged = {}, onNarrate = {})
        holderA.dslScreen.dataStore.rootState = stateA
        holderB.dslScreen.dataStore.rootState = stateB
        TestRoot.setFocus(null)

        stateA.setFocus(a.identity)
        assertTrue(a.isFocused, "a's own tree has it focused")
        assertFalse(b.isFocused, "b's tree must not see a's focus")

        // The main root is a third tree: what it focuses changes neither answer.
        TestRoot.setFocus(b.identity)
        assertTrue(a.isFocused, "a keeps answering from its own tree")
        assertFalse(b.isFocused, "b's own tree has nothing focused, whatever the main root focuses")

        stateB.setFocus(b.identity)
        assertTrue(b.isFocused, "b's own tree has it focused")
        assertTrue(a.isFocused, "both trees hold their own focus at the same time")

        stateA.setFocus(null)
        assertFalse(a.isFocused, "clearing a's tree unfocuses a")
        assertTrue(b.isFocused, "and leaves b's tree alone")
    }

    /**
     * A tree nobody mounted has no focus to report, and saying "not focused" would be a lie that hides
     * the real mistake. It fails loudly instead, and the message has to point at the cause.
     */
    @Test
    fun `a component of a tree that is not mounted fails loudly`() {
        val holder = TestBackend.create("unmounted") {
            EditableText(stringProp = editable("abc"), id = "box")
        }
        // Build the tree without mounting it: building is what creates the components.
        holder.dslScreen.build()
        val component = holder.dslScreen.children[0]
        assertNull(holder.dslScreen.dataStore.rootState, "nothing mounted this screen, so it has no tree")

        val focused = assertFailsWith<IllegalStateException>("focus must not be answered from nowhere") {
            component.isFocused
        }
        assertTrue(
            focused.message.orEmpty().contains("not mounted"),
            "the message must name the cause, was: ${focused.message}",
        )
        assertFailsWith<IllegalStateException>("hover must not be answered from nowhere") { component.isHovered }
    }
}
