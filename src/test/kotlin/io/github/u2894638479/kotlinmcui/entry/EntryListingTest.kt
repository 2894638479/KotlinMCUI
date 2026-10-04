package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.DslBackendMetadata
import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslMetadataProvider
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A class may only be registered under an interface it implements.
 *
 * The instance cache is shared between the service files and keyed by class alone, so a
 * class that is already cached must still be checked before it is handed to the caller
 * of the file that lists it. A real startup reaches exactly this state: reading the
 * common file caches its entries, and the client file is read straight afterwards.
 *
 * The case discovers both files itself and compares counters as deltas, so it does not
 * depend on another class having run first.
 */
class EntryListingTest {

    private var previousMetadataProvider: (() -> DslBackendMetadata)? = null

    @BeforeEach
    fun setUp() {
        previousMetadataProvider = dslMetadataProvider
    }

    @AfterEach
    fun tearDown() {
        dslMetadataProvider = previousMetadataProvider
    }

    @Test
    fun `a class listed under an interface it does not implement is skipped, not handed on`() {
        // Deliberately without DslRuntime.install: the point is the raw state a startup
        // is in after the common file has been read and before the client one is.
        dslMetadataProvider = { TestMetadata }
        DslEntryLoader.discover(Environment.COMMON)

        val clientRuns = FixtureSilentClient.runs

        DslEntryLoader.discover(Environment.CLIENT)

        val common = DslEntryLoader.entries.first { it.first === FixtureCommon }.second
        assertFalse(common.isClient, "FixtureCommon is listed in the client file but is not a DslEntryClient")
        assertEquals(clientRuns + 1, FixtureSilentClient.runs, "the entries listed after it are still initialised")
    }

    @Test
    fun `a listing that cannot be loaded is recorded with a reason instead of only logged`() {
        dslMetadataProvider = { TestMetadata }
        DslEntryLoader.discover(Environment.CLIENT)

        val broken = DslEntryLoader.brokenEntries.associateBy { it.className }
        assertEquals(
            "does not implement DslEntryClient",
            broken["io.github.u2894638479.kotlinmcui.entry.FixtureCommon"]?.reason,
            "a class listed under the wrong hook says so"
        )
        assertTrue(
            broken.keys.any { it.endsWith("FixtureThrowingConstructor") },
            "a constructor that throws is recorded, not only logged"
        )
        assertTrue(
            broken.keys.any { it.endsWith("FixtureBrokenStaticInit") },
            "a static initialiser that throws is recorded, not only logged"
        )
        assertTrue(
            broken.values.all { it.hookName.isNotBlank() && it.reason.isNotBlank() },
            "every record says which service file and why"
        )
    }
}
