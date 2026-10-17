package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslDataStore
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.session
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The root-bounded `session` scope.
 *
 * Its key is the bare key (like `static`), but its boundary is the root: two screens on one root share
 * it, two roots never do, and `root.close()` releases it.
 *
 * A screen's session scope is bound when the screen is *created* (the store points at a root), so these
 * cases build the screen for the root they mount on - that is what a backend's per-root screen factory
 * does in production.
 */
class SessionScopeTest {

    private fun screenOn(root: DslRoot, tag: String, body: DslFunction): DslScreen =
        DslDataStore(TestPlatform, root, tag, {}, body).dslScreen

    private fun render(screen: DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    @Test
    fun `two screens on one root share a session slot`() {
        val root = TestRoot.newRoot()
        var readOnB: Int? = null

        val writer = screenOn(root, "writer") {
            val slot = session("x") { 1 }
            Spacer(Modifier.size(slot.value.px, 10.px), "spacer")
        }
        root.children.collect(writer)
        render(writer)

        val reader = screenOn(root, "reader") {
            // Different screen, different declaration site: it must see what the first one set.
            val slot = session("x") { 0 }
            readOnB = slot.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(reader)
        render(reader)

        assertEquals(1, readOnB, "the session slot is a named address inside the root, not inside the screen")
    }

    @Test
    fun `two roots do not share session slots`() {
        val rootA = TestRoot.newRoot()
        val rootB = TestRoot.newRoot()
        var readOnB: Int? = null

        val a = screenOn(rootA, "a") {
            session("x") { 7 }
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        rootA.children.collect(a)
        render(a)

        val b = screenOn(rootB, "b") {
            val slot = session("x") { 0 }
            readOnB = slot.value
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        rootB.children.collect(b)
        render(b)

        assertEquals(0, readOnB, "the session scope is bounded by the root, so another root starts clean")
    }

    @Test
    fun `closing a root runs the session cleanups and empties the scope`() {
        val root = TestRoot.newRoot()
        var cleaned = 0

        val screen = screenOn(root, "session") {
            session("value") { 1 }
            session.dispose("cleanup") { cleaned++ }
            Spacer(Modifier.size(10.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(screen)

        assertEquals(2, root.sessionPropertyMap.size, "both slots live on the root")
        assertEquals(0, cleaned)

        root.close()

        assertEquals(1, cleaned, "close must run each slot's cleanup action")
        assertEquals(0, root.sessionPropertyMap.size, "and empty the scope")
        assertTrue(root.isClosed)
    }
}
