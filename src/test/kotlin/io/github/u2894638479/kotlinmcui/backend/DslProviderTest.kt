package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.config.configFormat
import io.github.u2894638479.kotlinmcui.config.loadConfig
import io.github.u2894638479.kotlinmcui.config.saveConfig
import io.github.u2894638479.kotlinmcui.integration.TestMetadata
import io.github.u2894638479.kotlinmcui.integration.TestPlatform
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class DslProviderTest {

    @Serializable
    data class Marker(val value: String = "")

    /** A provider that is nothing but metadata: reading it touches no platform. */
    private object OnlyMetadata : DslPlatformMetadata {
        override val configDir = Paths.get("build", "test-config-metadata")
        override val gameDir = Paths.get("build")
        override val gameVersion = "test"
        override val gameLoader = "test"
        override val environment = Environment.STANDALONE
    }

    private var previousMetadata: (() -> DslPlatformMetadata)? = null

    @BeforeEach
    fun setUp() {
        TestRuntime.install()
        previousMetadata = dslMetadataProvider
        Files.createDirectories(OnlyMetadata.configDir)
    }

    @AfterEach
    fun tearDown() {
        dslMetadataProvider = previousMetadata
        Files.walk(OnlyMetadata.configDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        // Leave the shared runtime the way the other cases expect to find it.
        TestRuntime.install()
    }

    @Test
    fun `reading metadata does not build the backend`() {
        dslMetadataProvider = { OnlyMetadata }

        assertEquals(OnlyMetadata.configDir, dslMetadata.configDir)
        saveConfig("marker.json", Marker("no backend"), configFormat)
        assertEquals(Marker("no backend"), loadConfig("marker.json", configFormat) { Marker() })
    }

    @Test
    fun `metadata is readable while no backend exists at all`() {
        dslMetadataProvider = { OnlyMetadata }

        assertEquals(OnlyMetadata.configDir, dslMetadata.configDir)
    }

    @Test
    fun `a named config file lands under the metadata config directory`() {
        dslMetadataProvider = { OnlyMetadata }
        saveConfig("nested/marker.json", Marker("nested"), configFormat)

        assertTrue(OnlyMetadata.configDir.resolve("nested/marker.json").exists())
    }

    /**
     * Metadata is installed, not derived: publishing the utils does not make metadata readable, so
     * the only source is the provider `DslRuntime.install` registered.
     */
    @Test
    fun `metadata comes only from install, never from the published utils`() {
        val previous = TestRuntime.publish()
        try {
            dslMetadataProvider = null
            val failure = assertFailsWith<IllegalStateException> { dslMetadata }
            assertTrue(
                failure.message.orEmpty().contains("install"),
                "the message must say install has not run, was: ${failure.message}",
            )
        } finally {
            TestRuntime.restore(previous)
        }
    }

    /** The published object is the one that answers, and it answers as the process-level helpers. */
    @Test
    fun `the published utils answer translation`() {
        val previous = TestRuntime.publish()
        try {
            assertSame(TestPlatform, dslUtils, "the published utils are the ones that answer")
            assertEquals("a.key", dslUtils.translate("a.key"), "translation comes from the published utils")
        } finally {
            TestRuntime.restore(previous)
        }
    }
}
