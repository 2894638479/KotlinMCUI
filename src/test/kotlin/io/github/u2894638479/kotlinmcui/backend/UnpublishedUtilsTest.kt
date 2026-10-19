package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * What the framework answers between `install` and the backend's `start`.
 *
 * Entry initialisation runs there, before any backend is ready, so the placeholder has to answer every
 * process-level query benignly: a translation falls back to its key, the world is not loaded, there is no
 * image yet. A session dispatcher is deliberately not here - it belongs to a root.
 */
class UnpublishedUtilsTest {
    private var previous: (() -> DslPlatformUtils)? = null
    private var previousMetadata: (() -> DslPlatformMetadata)? = null

    @BeforeTest
    fun setUp() {
        TestRuntime.install()
        previous = dslUtilsProvider
        dslUtilsProvider = null
        previousMetadata = dslMetadataProvider
    }

    @AfterTest
    fun tearDown() {
        dslUtilsProvider = previous
        dslMetadataProvider = previousMetadata
    }

    @Test
    fun `every member of the placeholder is benign`() {
        assertSame(UnpublishedUtils, dslUtils, "nothing published means the placeholder answers")
        assertEquals(null, dslUtils.translate("anything"))
        assertFalse(dslUtils.isInWorld)
        assertTrue(dslUtils.localImage(File("missing.png")).isEmpty)
        assertTrue(dslUtils.remoteImage("https://example.invalid/x.png").isEmpty)
        // The reloads are commands: running them must not throw either.
        dslUtils.reloadLocalImage(File("missing.png"))
        dslUtils.reloadRemoteImage("https://example.invalid/x.png")
    }

    @Test
    fun `metadata is readable as soon as install ran`() {
        assertEquals(TestMetadata, dslMetadata)
    }

    @Test
    fun `metadata is an error before install ran`() {
        dslMetadataProvider = null

        val error = assertFailsWith<IllegalStateException> { dslMetadata }

        assertTrue(
            error.message.orEmpty().contains("install"),
            "the message must say install has not run, was: " + error.message,
        )
    }
}
