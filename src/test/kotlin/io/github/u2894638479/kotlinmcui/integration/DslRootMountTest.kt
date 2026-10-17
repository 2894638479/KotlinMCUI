package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.container.DslRootGeometry
import io.github.u2894638479.kotlinmcui.container.DslRootState
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Mounting a screen wires it to the root it was mounted on.
 *
 * This closes the producer gap left by the focus work: `DslDataStore.rootState` used to have no
 * writer at all, so every component fell back to the main root. These cases hold the two ends of the
 * rule - a mount always stamps, and a screen moved to another root is stamped with that root.
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
        TopComponent.setFocus(null)
        TestBackend.resetPreview()
        dslBackendProvider = previous
    }

    private fun state() = DslRootState(onFocusChanged = { _, _ -> }, onHoverChanged = {}, onNarrate = {})

    private fun page() = TestBackend.create("page") { Spacer(Modifier.size(10.px, 10.px), "spacer") }.dslScreen

    @Test
    fun `mounting stamps the screen with the root it was mounted on`() {
        val screen = page()
        assertNull(screen.dataStore.rootState, "a screen has no tree before it is mounted")

        val root = DslRoot(state(), DslRootGeometry())
        root.children.collect(screen)

        assertSame(root.state, screen.dataStore.rootState, "the store now points at the root's state")
    }

    @Test
    fun `collectAll stamps every screen it adds`() {
        val root = DslRoot(state(), DslRootGeometry())
        val screen = page()

        root.children.collectAll(listOf(screen))

        assertSame(root.state, screen.dataStore.rootState, "a bulk mount stamps like a single one")
    }

    @Test
    fun `a screen moved to another root is stamped with the new root`() {
        val rootA = DslRoot(state(), DslRootGeometry())
        val rootB = DslRoot(state(), DslRootGeometry())
        val screen = page()

        val child = rootA.children.collect(screen)
        assertSame(rootA.state, screen.dataStore.rootState)

        // Taking it off does not clear the stamp: a hidden screen is not a destroyed one.
        rootA.children.remove(child)
        assertSame(rootA.state, screen.dataStore.rootState, "removing keeps the last owner's state")

        rootB.children.collect(screen)
        assertSame(rootB.state, screen.dataStore.rootState, "the new root takes over the stamp")
    }
}
