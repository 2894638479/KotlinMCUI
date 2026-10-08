package io.github.u2894638479.kotlinmcui.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * The language files are read by Minecraft's own strict JSON reader: one trailing comma, or a
 * raw newline inside a string, makes it skip the whole file and every translation silently
 * falls back to its key. PowerShell's `ConvertFrom-Json` accepts a trailing comma, so this
 * parses them the strict way and compares the two files key by key.
 */
class LangFileTest {

    private fun load(name: String): Map<String, String> {
        val text = javaClass.getResourceAsStream("/assets/kotlinmcui/lang/$name")
            ?.reader()?.readText()
            ?: fail("missing language file $name")
        return Json.parseToJsonElement(text).jsonObject.mapValues { it.value.toString() }
    }

    @Test
    fun `both language files parse strictly and carry the same keys`() {
        val en = load("en_us.json")
        val zh = load("zh_cn.json")

        assertTrue(en.isNotEmpty(), "en_us.json has entries")
        assertEquals(en.keys, zh.keys, "the two language files must translate the same keys")
        assertTrue(en.keys.all { it.startsWith("kotlinmcui.") }, "every key is namespaced")
    }
}
