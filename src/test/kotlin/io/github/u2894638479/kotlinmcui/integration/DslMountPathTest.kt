package io.github.u2894638479.kotlinmcui.integration

import io.github.u2894638479.kotlinmcui.backend.DslBackend
import io.github.u2894638479.kotlinmcui.backend.dslBackendProvider
import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.TopComponent
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * The root's child list starts with the overlay screen that [TopComponent.children] builds on first
 * use, and a mounted page follows it.
 *
 * Nothing else asserts this: deleting the `collect` from that lazy initializer leaves the whole suite
 * green (measured), even though the framework's own debug overlay reads the mounted page at a fixed
 * index. This case is what makes that deletion fail.
 *
 * Falsifiable: remove the `collect` call from the `TopComponent.children` initializer and the first
 * assertion fails, because the root then starts out empty.
 */
class DslMountPathTest {
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
        TestBackend.resetPreview()
        dslBackendProvider = previous
    }

    @Test
    fun `the overlay screen is the root's first child`() {
        val overlay = assertIs<DslScreen>(TopComponent.children.firstOrNull(), "the root starts with the overlay screen")
        assertEquals("Dsl Overlay", overlay.dataStore.title)

        // A mounted page lands after the overlay, which is the index the debug overlay reads.
        val holder = TestBackend.create("page") { Spacer(Modifier.size(10.px, 10.px), "spacer") }
        holder.show()
        assertEquals(
            listOf("Dsl Overlay", "page"),
            TopComponent.children.map { (it as DslScreen).dataStore.title },
            "a shown page is appended after the overlay"
        )
    }
}
