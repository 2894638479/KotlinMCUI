package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.config.configFormat
import io.github.u2894638479.kotlinmcui.config.loadConfig
import io.github.u2894638479.kotlinmcui.config.saveConfig
import io.github.u2894638479.kotlinmcui.integration.TestBackend
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
    private object OnlyMetadata : DslBackendMetadata {
        override val configDir = Paths.get("build", "test-config-metadata")
        override val gameDir = Paths.get("build")
        override val gameVersion = "test"
        override val gameLoader = "test"
        override val environment = Environment.COMMON
    }

    @BeforeEach
    fun setUp() {
        Files.createDirectories(OnlyMetadata.configDir)
    }

    @AfterEach
    fun tearDown() {
        dslMetadataProvider = null
        Files.walk(OnlyMetadata.configDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        // Leave the shared platform the way the other cases expect to find it.
        TestRuntime.install()
    }

    @Test
    fun `reading metadata does not build the backend`() {
        dslMetadataProvider = { OnlyMetadata }
        dslBackendProvider = { error("metadata must not need the backend") }

        assertEquals(OnlyMetadata.configDir, dslMetadata.configDir)
        saveConfig("marker.json", Marker("no backend"), configFormat)
        assertEquals(Marker("no backend"), loadConfig("marker.json", configFormat) { Marker() })
    }

    @Test
    fun `metadata is readable while no backend exists at all`() {
        dslMetadataProvider = { OnlyMetadata }
        dslBackendProvider = null

        assertEquals(OnlyMetadata.configDir, dslMetadata.configDir)
        assertFailsWith<IllegalStateException>("the backend itself must still be unavailable") { dslBackend }
    }

    @Test
    fun `a named config file lands under the metadata config directory`() {
        dslMetadataProvider = { OnlyMetadata }
        saveConfig("nested/marker.json", Marker("nested"), configFormat)

        assertTrue(OnlyMetadata.configDir.resolve("nested/marker.json").exists())
    }

    @Test
    fun `metadata falls back to the backend when no provider is installed`() {
        dslMetadataProvider = null
        dslBackendProvider = { TestBackend }
        assertEquals(TestBackend.configDir, dslMetadata.configDir)
    }
}
