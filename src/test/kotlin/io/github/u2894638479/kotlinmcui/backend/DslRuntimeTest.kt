package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.entry.FixtureStats
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder

/**
 * The process-wide startup sequence, case by case.
 *
 * The runtime owns exactly two facts now: whether the platform is installed, and whether it has been
 * published. It owns no roots - a root belongs to the backend that built it - so there is nothing here
 * about roots, owners or refusals.
 *
 * `resetForTest` puts the singleton back into its initial state before each case, which is why the
 * cases can assert on the sequence: they all start from "nothing installed", and JUnit runs the
 * methods in the declared order so the install-once cases read like the real startup.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DslRuntimeTest {
    @BeforeEach
    fun setUp() = DslRuntime.resetForTest()

    @AfterEach
    fun tearDown() = DslRuntime.resetForTest()

    @Test
    @Order(1)
    fun `publishing the platform before installing it is an error`() {
        assertFalse(DslRuntime.isInstalled)
        assertFailsWith<IllegalStateException> { DslRuntime.start { TestPlatform } }
    }

    @Test
    @Order(2)
    fun `installing discovers the entries and initialises them exactly once`() {
        val before = FixtureStats.stages.size

        DslRuntime.install { TestMetadata }

        assertTrue(
            FixtureStats.stages.size > before,
            "installing must run the initialize stage of the entries that apply to this environment",
        )
    }

    @Test
    @Order(3)
    fun `metadata works before the platform is ready while the platform does not`() {
        DslRuntime.install { TestMetadata }

        assertEquals(TestMetadata.configDir, dslMetadata.configDir)

        val failure = assertFailsWith<IllegalStateException> { dslPlatform }
        assertTrue(
            "not ready" in failure.message.orEmpty(),
            "the failure must say the platform is not ready yet, was: ${failure.message}"
        )
    }

    @Test
    @Order(4)
    fun `publishing the platform makes it available`() {
        DslRuntime.install { TestMetadata }
        DslRuntime.start { TestPlatform }

        assertSame(TestPlatform, dslPlatform)
    }

    @Test
    @Order(5)
    fun `installing a second time is an error`() {
        DslRuntime.install { TestMetadata }

        assertFailsWith<IllegalStateException> { DslRuntime.install { TestMetadata } }
    }

    @Test
    @Order(6)
    fun `publishing a second platform is refused, not thrown`() {
        DslRuntime.install { TestMetadata }
        val first = DslRuntime.start { TestPlatform }

        // Refusing instead of throwing keeps a user's double install from becoming a crash,
        // so the caller has to learn that it lost from the return value.
        val second = DslRuntime.start { TestPlatform }

        assertTrue(first, "the first publish must report success")
        assertFalse(second, "a later publish must report that it was refused")
        assertSame(TestPlatform, dslPlatform, "the refused platform must not replace the published one")
    }
}
