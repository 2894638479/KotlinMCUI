package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.integration.TestRuntime
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir

/**
 * The framework's own entries are registered from `src/main/resources` as well, so
 * these cases look only at the fixtures declared in `src/test/resources`.
 *
 * The installed platform is JVM-wide ([TestRuntime]), which is what the loader's
 * single-shot discovery needs; `DslRuntimeTest` owns the sequence itself.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DslEntryLoaderTest {

    private val configPath get() = overlayConfigPath()

    /** JUnit creates and removes this one, so the cases leave nothing behind. */
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        TestRuntime.install()
        Files.deleteIfExists(configPath)
        // The loader is process-wide, so earlier cases may have left overlays on.
        DslEntryLoader.enabledOverlays.forEach { DslEntryLoader.setOverlayEnabled(it,false) }
        // The stored config is a case's own business: start each one without a file.
        Files.deleteIfExists(configPath)
    }

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(configPath)
    }

    // ── loading ──────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    fun `entries register whether they are declared as objects or as classes`() {
        val registered = DslEntryLoader.entries.map { it.first::class }
        assertTrue(FixtureGuiObject::class in registered, "a Kotlin object entry must register")
        assertTrue(FixtureGuiClass::class in registered, "an entry with a public no-argument constructor must register")
    }

    @Test
    @Order(3)
    fun `flags record how an entry was discovered`() {
        val gui = DslEntryLoader.entries.first { it.first === FixtureGuiObject }
        assertTrue(gui.second.isGui)
        assertFalse(gui.second.isOverlay)

        val overlay = DslEntryLoader.entries.first { it.first === FixtureOverlay }
        assertTrue(overlay.second.isOverlay)
        assertFalse(overlay.second.isGui)
    }

    @Test
    @Order(4)
    fun `a broken entry does not stop the ones around it`() {
        val registered = DslEntryLoader.entries.map { it.first::class }

        // Both broken fixtures sit between two healthy ones in the service file, and
        // the second one fails with an Error instead of an exception.
        assertTrue(FixtureGuiObject::class in registered)
        assertTrue(FixtureGuiClass::class in registered, "an entry after the broken ones must still register")
        assertFalse(FixtureThrowingConstructor::class in registered, "an entry whose constructor fails is skipped")
        assertFalse(FixtureBrokenStaticInit::class in registered, "an entry whose static initialiser fails is skipped")
    }

    // ── the stored overlay list ──────────────────────────────────────────────

    @Test
    @Order(5)
    fun `ids are stored and read back in order`() {
        val file = tempDir.resolve("overlays.json")
        writeOverlayIds(listOf("a","b","c"),file)
        assertEquals(listOf("a","b","c"), readOverlayIds(file))
    }

    @Test
    @Order(6)
    fun `a missing file reads as no overlays`() {
        val file = tempDir.resolve("absent.json")
        assertEquals(emptyList(), readOverlayIds(file))
    }

    @Test
    @Order(7)
    fun `a malformed file reads as no overlays`() {
        val file = tempDir.resolve("broken.json")
        file.writeText("{ not json")
        assertEquals(emptyList(), readOverlayIds(file))
    }

    @Test
    @Order(8)
    fun `enabling an overlay writes the config`() {
        assertFalse(configPath.exists(), "the case starts without a config file")

        DslEntryLoader.setOverlayEnabled(FixtureOverlay,true)

        assertTrue(DslEntryLoader.isOverlayEnabled(FixtureOverlay))
        assertEquals(listOf(FixtureOverlay), DslEntryLoader.enabledOverlays)
        assertTrue(configPath.exists(), "enabling an overlay must write the config")
        assertTrue("\"${FixtureOverlay.id}\"" in configPath.readText(), "the stored file holds the overlay id")
    }

    @Test
    @Order(9)
    fun `disabling an overlay removes it from the config`() {
        DslEntryLoader.setOverlayEnabled(FixtureOverlay,true)
        DslEntryLoader.setOverlayEnabled(FixtureOverlay,false)

        assertFalse(DslEntryLoader.isOverlayEnabled(FixtureOverlay))
        assertEquals(emptyList(), DslEntryLoader.enabledOverlays)
        assertFalse("\"${FixtureOverlay.id}\"" in configPath.readText())
    }

    @Test
    @Order(10)
    fun `bringing an overlay to the top reorders the stored list`() {
        val other = DslEntryLoader.entries
            .filter { it.second.isOverlay }
            .map { it.first as DslEntryOverlay }
            .first { it.id != FixtureOverlay.id }

        DslEntryLoader.setOverlayEnabled(other,true)
        DslEntryLoader.setOverlayEnabled(FixtureOverlay,true)
        assertEquals(listOf(other,FixtureOverlay), DslEntryLoader.enabledOverlays)

        DslEntryLoader.bringOverlayToTop(FixtureOverlay)
        assertEquals(listOf(FixtureOverlay,other), DslEntryLoader.enabledOverlays)
        assertEquals(listOf(FixtureOverlay.id,other.id), readOverlayIds())
    }

    @Test
    @Order(11)
    fun `an id whose mod is not installed stays in the file and renders nothing`() {
        val ghost = "absentmod:ghost"
        Files.createDirectories(configPath.parent)
        configPath.writeText("""{ "enabled": [ "$ghost" ] }""")

        DslEntryLoader.discover(Environment.COMMON)
        assertEquals(emptyList(), DslEntryLoader.enabledOverlays, "an id with no overlay behind it must not render")
        assertEquals(listOf(ghost), readOverlayIds(), "but the stored list keeps it")

        DslEntryLoader.setOverlayEnabled(FixtureOverlay,true)
        assertTrue(ghost in readOverlayIds(), "rewriting the list must not drop the id of the absent mod")
    }
}
