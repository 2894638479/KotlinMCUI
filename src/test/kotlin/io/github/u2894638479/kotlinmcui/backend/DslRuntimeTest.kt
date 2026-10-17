package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.FixtureClient
import io.github.u2894638479.kotlinmcui.entry.FixtureCommon
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRootProvider
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

    /**
     * The owner count is process-lifetime and is not reset with the runtime: the main root is a
     * lazily built singleton, so whichever case touches it first creates it, and it is created
     * through the only path that may set `isOwner`.
     */
    @Test
    @Order(1)
    fun `the main root is the process owner`() {
        DslRuntime.install { TestMetadata }
        DslRuntime.platformReady { TestPlatform }
        DslRuntime.registerProvider(TestRootProvider)

        val root = DslRuntime.ownerRoot

        assertTrue(root.isOwner, "the main root must be created through createOwner")
        assertEquals(1, DslRuntime.ownerCreations, "exactly one owner root is built")
    }

    @Test
    @Order(2)
    fun `the owner root draws with the registered provider`() {
        DslRuntime.install { TestMetadata }
        DslRuntime.platformReady { TestPlatform }
        DslRuntime.registerProvider(TestRootProvider)

        assertSame(
            TestRootProvider,
            DslRuntime.ownerRoot.provider,
            "the main root must use the provider an entry point registered, not a copy",
        )
    }

    @Test
    @Order(2)
    fun `publishing the platform before installing it is an error`() {
        assertFalse(DslRuntime.isInstalled)
        assertFailsWith<IllegalStateException> { DslRuntime.platformReady { TestPlatform } }
    }

    @Test
    @Order(3)
    fun `installing discovers the entries and initialises them exactly once`() {
        val commonRuns = FixtureCommon.runs
        val clientRuns = FixtureClient.runs

        DslRuntime.install { TestMetadata }

        assertEquals(commonRuns + 1, FixtureCommon.runs, "installing must initialise the common entries once")
        assertEquals(clientRuns, FixtureClient.runs, "a common platform must not run the client initialisation")
    }

    @Test
    @Order(4)
    fun `metadata works before the platform is ready while the backend does not`() {
        DslRuntime.install { TestMetadata }

        assertEquals(TestMetadata.configDir, dslMetadata.configDir)

        val failure = assertFailsWith<IllegalStateException> { dslPlatform }
        assertTrue(
            "not ready" in failure.message.orEmpty(),
            "the failure must say the platform is not ready yet, was: ${failure.message}"
        )
    }

    @Test
    @Order(5)
    fun `publishing the platform makes it available`() {
        DslRuntime.install { TestMetadata }
        DslRuntime.platformReady { TestPlatform }

        assertSame(TestPlatform, dslPlatform)
    }

    @Test
    @Order(6)
    fun `installing a second time is an error`() {
        DslRuntime.install { TestMetadata }

        assertFailsWith<IllegalStateException> { DslRuntime.install { TestMetadata } }
    }

    @Test
    @Order(7)
    fun `publishing a second platform is refused, not thrown`() {
        DslRuntime.install { TestMetadata }
        val first = DslRuntime.platformReady { TestPlatform }

        // Refusing instead of throwing keeps a user's double install from becoming a crash,
        // so the caller has to learn that it lost from the return value.
        val second = DslRuntime.platformReady { TestPlatform }

        assertTrue(first, "the first publish must report success")
        assertFalse(second, "a later publish must report that it was refused")
        assertSame(TestPlatform, dslPlatform, "the refused platform must not replace the published one")
    }

    @Test
    @Order(8)
    fun `a second owner root is refused and recorded in the ledger`() {
        DslRuntime.install { TestMetadata }
        DslRuntime.platformReady { TestPlatform }
        val provider = TestRootProvider

        val first = DslRuntime.createOwner(provider)
        val second = DslRuntime.createOwner(provider)

        assertTrue(first.isOwner, "the first owner root is the owner")
        with(DslRuntime) {
            assertTrue(first.isRegistered, "the first owner root works")
            assertFalse(second.isRegistered, "the second owner root must be refused")
            assertEquals(DslRuntime.DslRejectionReason.PLATFORM_ALREADY_HELD, second.refusalReason)
        }
        assertTrue(
            DslRuntime.rejections.any { it.reason == DslRuntime.DslRejectionReason.PLATFORM_ALREADY_HELD },
            "the refusal must be recorded in the ledger",
        )
    }
}
