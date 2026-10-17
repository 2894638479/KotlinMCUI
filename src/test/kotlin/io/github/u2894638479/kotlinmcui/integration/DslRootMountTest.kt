package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.DslRuntime
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslRootGeometry
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.dsl.decorator.tooltip
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.text.CodePoints
import io.github.u2894638479.kotlinmcui.widget.Presets
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Mounting a screen wires it to the root it was mounted on, and the screen's readers then report that
 * root.
 *
 * This closes the producer gap left by the focus work: `DslDataStore.rootState` used to have no
 * writer at all, so every component fell back to the main root. The cases hold both ends of the rule -
 * a mount always stamps (a moved screen takes the new root), and the screen's own IME box, tooltip and
 * debug overlay answer from that stamp rather than from the main root.
 */
class DslRootMountTest {
    /** Process-wide state: put back whatever provider was published before the case. */
    private var previous: (() -> DslBackend<*, *>)? = null

    @BeforeEach
    fun setUp() {
        previous = dslBackendProvider
        dslBackendProvider = { TestBackend }
        TestBackend.resetPreview()
    }

    @AfterEach
    fun tearDown() {
        TestRoot.setFocus(null)
        TestBackend.resetPreview()
        dslBackendProvider = previous
    }

    private fun state() = DslRootState(onFocusChanged = { _, _ -> }, onHoverChanged = {}, onNarrate = {})

    private fun page() = TestBackend.create("page") { Spacer(Modifier.size(10.px, 10.px), "spacer") }.dslScreen

    private fun editable(initial: String): StableRW<String> {
        var text = initial
        return object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
    }

    /** Render the screen without mounting it: its store still points at the root that collected it. */
    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    @Test
    fun `mounting stamps the screen with the root it was mounted on`() {
        val screen = page()
        assertNull(screen.dataStore.rootState, "a screen has no tree before it is mounted")

        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        root.children.collect(screen)

        assertSame(root.state, screen.dataStore.rootState, "the store now points at the root's state")
    }

    @Test
    fun `collectAll stamps every screen it adds`() {
        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        val screen = page()

        root.children.collectAll(listOf(screen))

        assertSame(root.state, screen.dataStore.rootState, "a bulk mount stamps like a single one")
    }

    @Test
    fun `a screen moved to another root is stamped with the new root`() {
        val rootA = DslRoot(state(), DslRootGeometry(), TestBackend)
        val rootB = DslRoot(state(), DslRootGeometry(), TestBackend)
        val screen = page()

        val child = rootA.children.collect(screen)
        assertSame(rootA.state, screen.dataStore.rootState)

        // Taking it off does not clear the stamp: a hidden screen is not a destroyed one.
        rootA.children.remove(child)
        assertSame(rootA.state, screen.dataStore.rootState, "removing keeps the last owner's state")

        rootB.children.collect(screen)
        assertSame(rootB.state, screen.dataStore.rootState, "the new root takes over the stamp")
    }

    @Test
    fun `the runtime hands out the main root that TestRoot stands for`() {
        val root = DslRuntime.ownerRoot

        assertSame(root.children, TestRoot.children, "both hand out the same root's children")
        assertSame(root.component, TestRoot.instance, "both hand out the same root component")
    }

    @Test
    fun `the debug overlay reports the focus of its own tree`() {
        val screen = page()
        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        root.children.collect(screen)
        TestRoot.setFocus(null)
        val target = DslId("target")

        root.state.setFocus(target)

        assertEquals(target, screen.dataStore.debugOverlay.focusedId, "the overlay reads its own root")
    }

    @Test
    fun `preeditUsing reports whether the focused component accepts text input`() {
        val screen = TestBackend.create("ime") {
            Column {
                EditableText(stringProp = editable("abc"), preeditSelfRendered = false, id = "editor")
                Spacer(Modifier.size(10.px, 10.px), "plain")
            }
        }.dslScreen
        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        root.children.collect(screen)
        TestBackend.mouseAt(Position(500.px, 500.px))
        render(screen)
        val column = screen.children[0]
        val editor = column.children[0]
        val plain = column.children[1]

        assertFalse(root.preeditUsing, "nothing is focused")

        root.state.setFocus(editor.identity)
        assertTrue(root.preeditUsing, "the focused editor accepts text input")

        root.state.setFocus(plain.identity)
        assertFalse(root.preeditUsing, "a spacer has no preedit area")
    }

    @Test
    fun `the tooltip is built from the focus of its own tree`() {
        val screen = TestBackend.create("tooltip") {
            Spacer(Modifier.size(10.px, 10.px), "target").tooltip { Presets.Text { "tip" } }
        }.dslScreen
        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        root.children.collect(screen)
        TestRoot.setFocus(null)
        // Keep the pointer off the component: only the focused path may build a tooltip layer here.
        TestBackend.mouseAt(Position(500.px, 500.px))
        render(screen)
        val target = screen.children[0]
        val withoutFocus = screen.children.size

        root.state.setFocus(target.identity)
        render(screen)

        assertEquals(withoutFocus + 1, screen.children.size, "the tooltip layer comes from its own root")
    }

    @Test
    fun `the IME box is built from the preedit of its own tree`() {
        val screen = TestBackend.create("ime") {
            // Let the framework draw the IME box: an editor that renders its own preedit has none.
            EditableText(stringProp = editable("abc"), preeditSelfRendered = false, id = "box")
        }.dslScreen
        val root = DslRoot(state(), DslRootGeometry(), TestBackend)
        root.children.collect(screen)
        TestRoot.setFocus(null)
        TestBackend.mouseAt(Position(500.px, 500.px))
        render(screen)
        val box = screen.children[0]
        val withoutPreedit = screen.children.size

        root.state.setFocus(box.identity)
        root.state.setPreedit(PreeditInfo(listOf(CodePoints.Const("ni")), 0, 2))
        render(screen)

        assertEquals(withoutPreedit + 1, screen.children.size, "the IME box comes from its own root")
    }
}
