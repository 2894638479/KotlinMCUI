package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.FixtureClient
import io.github.u2894638479.kotlinmcui.entry.FixtureCommon
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import kotlin.test.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * The startup sequence is a process-wide, single-shot thing, so this class owns it:
 * `@Order(1)` puts it before every other test class (see `junit-platform.properties`),
 * and the cases below walk the sequence in order.
 */
@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DslRuntimeTest {

    @Test
    @Order(1)
    fun `publishing the backend before installing the platform is an error`() {
        assertFalse(DslRuntime.isInstalled)
        assertFailsWith<IllegalStateException> { DslRuntime.platformReady({ TestBackend }) }
    }

    @Test
    @Order(2)
    fun `installing discovers the entries and initialises them exactly once`() {
        DslRuntime.install({ TestMetadata })

        assertEquals(1, FixtureCommon.runs)
        assertEquals(0, FixtureClient.runs, "a common platform must not run the client initialisation")
    }

    @Test
    @Order(3)
    fun `metadata works before the platform is ready while the backend does not`() {
        assertEquals(TestMetadata.configDir, dslMetadata.configDir)

        val failure = assertFailsWith<IllegalStateException> { dslBackend }
        assertTrue(
            "not ready" in failure.message.orEmpty(),
            "the failure must say the platform is not ready yet, was: ${failure.message}"
        )
    }

    @Test
    @Order(4)
    fun `publishing the backend makes it available`() {
        DslRuntime.platformReady({ TestBackend })

        assertSame(TestBackend, dslBackend)
    }

    @Test
    @Order(5)
    fun `installing a second time is an error`() {
        assertFailsWith<IllegalStateException> { DslRuntime.install({ TestMetadata }) }
    }

    @Test
    @Order(6)
    fun `publishing a second backend is an error`() {
        assertFailsWith<IllegalStateException> { DslRuntime.platformReady({ TestBackend }) }
    }
}
