package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.DslBackendMetadata
import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.backend.dslMetadataProvider
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

/**
 * A class may only be registered under an interface it implements.
 *
 * The instance cache is shared between the service files and keyed by class alone, so a
 * class that is already cached must still be checked before it is handed to the caller
 * of the file that lists it. A real startup reaches exactly this state: reading the
 * common file caches its entries, and the client file is read straight afterwards.
 *
 * Runs after [EntryEnvironmentTest], whose "this class was never loaded" cases need to
 * see the loader before any client pass.
 */
@Order(3)
class EntryListingTest {

    private var previousMetadataProvider: (() -> DslBackendMetadata)? = null

    @AfterEach
    fun tearDown() {
        dslMetadataProvider = previousMetadataProvider
    }

    @Test
    fun `a class listed under an interface it does not implement is skipped, not handed on`() {
        // Deliberately without DslRuntime.install: the point is the raw state a startup
        // is in after the common file has been read and before the client one is.
        previousMetadataProvider = dslMetadataProvider
        dslMetadataProvider = { TestMetadata }
        DslEntryLoader.discover(Environment.COMMON)

        val clientRuns = FixtureSilentClient.runs

        DslEntryLoader.discover(Environment.CLIENT)

        val common = DslEntryLoader.entries.first { it.first === FixtureCommon }.second
        assertFalse(common.isClient, "FixtureCommon is listed in the client file but is not a DslEntryClient")
        assertEquals(clientRuns + 1, FixtureSilentClient.runs, "the entries listed after it are still initialised")
    }
}
