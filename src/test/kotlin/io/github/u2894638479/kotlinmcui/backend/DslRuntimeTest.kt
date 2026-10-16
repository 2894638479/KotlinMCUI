package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.FixtureClient
import io.github.u2894638479.kotlinmcui.entry.FixtureCommon
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * The startup sequence is a process-wide, single-shot thing, so every case first puts the
 * runtime back into the state the process starts in ([DslRuntime.resetForTest]) and then
 * arranges the part of the sequence it is about. Nothing here depends on which class ran
 * before, so neither a class-level `@Order` nor a class-order configuration is needed.
 *
 * The counters are compared as deltas for the same reason: installing runs entry
 * discovery, and discovery may already have run for any number of other cases.
 *
 * The method `@Order` is kept only because the cases read as the sequence a real startup
 * walks; no case builds on the state a previous one left behind.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DslRuntimeTest {

    @BeforeEach
    fun setUp() = DslRuntime.resetForTest()

    @Test
    @Order(1)
    fun `publishing the backend before installing the platform is an error`() {
        assertFalse(DslRuntime.isInstalled)
        assertFailsWith<IllegalStateException> { DslRuntime.platformReady { TestBackend } }
    }

    @Test
    @Order(2)
    fun `installing discovers the entries and initialises them exactly once`() {
        val commonRuns = FixtureCommon.runs
        val clientRuns = FixtureClient.runs

        DslRuntime.install { TestMetadata }

        assertEquals(commonRuns + 1, FixtureCommon.runs, "installing must initialise the common entries once")
        assertEquals(clientRuns, FixtureClient.runs, "a common platform must not run the client initialisation")
    }

    @Test
    @Order(3)
    fun `metadata works before the platform is ready while the backend does not`() {
        DslRuntime.install { TestMetadata }

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
        DslRuntime.install { TestMetadata }
        DslRuntime.platformReady { TestBackend }

        assertSame(TestBackend, dslBackend)
    }

    @Test
    @Order(5)
    fun `installing a second time is an error`() {
        DslRuntime.install { TestMetadata }

        assertFailsWith<IllegalStateException> { DslRuntime.install { TestMetadata } }
    }

    @Test
    @Order(6)
    fun `publishing a second backend is refused, not thrown`() {
        DslRuntime.install { TestMetadata }
        val first = DslRuntime.platformReady { TestBackend }

        // Refusing instead of throwing keeps a user's double install from becoming a crash,
        // so the caller has to learn that it lost from the return value.
        val second = DslRuntime.platformReady { TestBackend }

        assertTrue(first, "the first publish must report success")
        assertFalse(second, "a later publish must report that it was refused")
        assertSame(TestBackend, dslBackend, "the refused backend must not replace the published one")
    }
}
