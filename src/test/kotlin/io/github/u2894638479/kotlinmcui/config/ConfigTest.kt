package io.github.u2894638479.kotlinmcui.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class ConfigTest {

    @Serializable
    data class Settings(val name: String = "unset", val order: List<String> = emptyList())

    private lateinit var dir: Path

    @BeforeEach
    fun setUp() {
        dir = Files.createTempDirectory("kotlinmcui-config-test")
    }

    @AfterEach
    fun tearDown() {
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }

    @Test
    fun `a saved config loads back with the order of its list`() {
        val path = dir.resolve("settings.json")
        saveConfig(path, Settings("gui", listOf("a", "b", "c")), configFormat)

        val loaded = loadConfig(path, configFormat) { Settings() }
        assertEquals(Settings("gui", listOf("a", "b", "c")), loaded)
        assertEquals(loaded.order, listOf("a", "b", "c"), "list order must survive the round trip")
    }

    @Test
    fun `saving fills in the missing directories`() {
        val path = dir.resolve("nested/deeper/settings.json")
        saveConfig(path, Settings("nested"), configFormat)
        assertTrue(path.exists(), "saving must create the parent directories")
        assertEquals("nested", loadConfig(path, configFormat) { Settings() }.name)
    }

    @Test
    fun `a missing file falls back to the default`() {
        val path = dir.resolve("absent.json")
        assertFalse(path.exists())
        assertEquals(Settings("fallback"), loadConfig(path, configFormat) { Settings("fallback") })
    }

    @Test
    fun `a malformed file falls back to the default and is left alone`() {
        val path = dir.resolve("broken.json")
        path.writeText("{ this is not json")
        assertEquals(Settings("fallback"), loadConfig(path, configFormat) { Settings("fallback") })
        assertEquals("{ this is not json", path.readText(), "a broken file is kept for inspection")
    }

    @Test
    fun `saving replaces an existing file and leaves no temporary file behind`() {
        val path = dir.resolve("settings.json")
        saveConfig(path, Settings("first"), configFormat)
        saveConfig(path, Settings("second"), configFormat)

        assertEquals("second", loadConfig(path, configFormat) { Settings() }.name)
        assertFalse(dir.resolve("settings.json.tmp").exists(), "the temporary file must be moved, not left")
        assertEquals(1, Files.list(dir).use { it.count() }.toInt(), "only the config itself is left")
    }

    @Test
    fun `an explicit serializer works for types that cannot be reified`() {
        val path = dir.resolve("numbers.json")
        val serializer: KSerializer<List<Int>> = ListSerializer(Int.serializer())
        saveConfig(path, listOf(3, 1, 2), configFormat, serializer)

        assertEquals(listOf(3, 1, 2), loadConfig(path, configFormat, { emptyList() }, serializer))
    }

    @Test
    fun `a save that cannot write is reported and thrown no further`() {
        // The path is a directory, so writing the temporary file next to it must fail.
        val path = dir.resolve("a-directory")
        Files.createDirectories(path)
        saveConfig(path, Settings("doomed"), configFormat)
        // Nothing to assert beyond "did not throw": the failure is logged by design.
    }
}
