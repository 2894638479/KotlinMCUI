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
import kotlin.test.assertTrue

/**
 * What the framework answers between `install` and the backend's `start`.
 *
 * Entry initialisation runs there, before any backend is ready, so the benign queries have to work: a
 * translation falls back to its key, the world is not loaded, there is no image yet. Dispatching onto the
 * session thread is the one thing a placeholder cannot do - it would need the thread a backend owns.
 */
class UnpublishedPlatformTest {
    private var previous: (() -> DslPlatform)? = null

    @BeforeTest
    fun setUp() {
        TestRuntime.install()
        previous = dslPlatformProvider
        dslPlatformProvider = null
    }

    @AfterTest
    fun tearDown() {
        dslPlatformProvider = previous
    }

    @Test
    fun `the benign queries answer before anything is published`() {
        assertEquals(null, dslPlatform.utils.translate("anything"))
        assertFalse(dslPlatform.utils.isInWorld)
        assertTrue(dslPlatform.utils.localImage(File("missing.png")).isEmpty)
        assertTrue(dslPlatform.utils.remoteImage("https://example.invalid/x.png").isEmpty)
        // The reloads are commands: running them must not throw either.
        dslPlatform.utils.reloadLocalImage(File("missing.png"))
        dslPlatform.utils.reloadRemoteImage("https://example.invalid/x.png")
    }

    @Test
    fun `metadata is readable as soon as install ran`() {
        assertEquals(TestMetadata, dslPlatform.metadata)
    }

    @Test
    fun `the session dispatcher is the one thing that cannot be faked`() {
        val error = assertFailsWith<IllegalStateException> { dslPlatform.dispatcher }

        assertTrue(
            error.message.orEmpty().contains("session thread"),
            "the message must say what is missing, was: " + error.message,
        )
    }
}
