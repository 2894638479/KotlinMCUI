package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.text.CodePoints
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * A composition from the platform is addressed to the focused field, and a hit test that finds no
 * match falls back to the component it was called on - which for this path is the root, a node with
 * no identity to compare against.
 */
class DslPreeditTest {

    /** Process-wide state: put back whatever provider was published before the case. */
    private var previousBackendProvider: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previousBackendProvider = dslBackendProvider
        dslBackendProvider = { TestBackend }
    }

    @AfterEach
    fun tearDown() {
        TopComponent.setFocus(null)
        TestBackend.resetPreview()
        dslBackendProvider = previousBackendProvider
    }

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

    private fun composing() = PreeditInfo(listOf(CodePoints.Const("ni")), 0, 2)

    @Test
    fun `a composition reaches the focused field`() {
        val screen = buildScreen { EditableText(stringProp = editable("abc"), id = "box") }
        TopComponent.setFocus(screen.children[0].identity)

        TopComponent.globalPreedit(composing())

        assertFalse(TopComponent.preedit.isEmpty, "the composition is recorded")
    }

    @Test
    fun `a composition nobody is focused for is still recorded`() {
        buildScreen { EditableText(stringProp = editable("abc"), id = "box") }

        TopComponent.globalPreedit(composing())

        assertFalse(TopComponent.preedit.isEmpty, "the composition is recorded")
    }
}
