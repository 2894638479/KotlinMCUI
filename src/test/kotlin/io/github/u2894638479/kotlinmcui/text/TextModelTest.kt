package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.prop.StableRW
import kotlin.test.*

class TextModelTest {

    private fun editable(initial: String): Pair<EditableString, () -> String> {
        var text = initial
        val prop = object : StableRW<String> {
            override fun getValue() = text
            override fun setValue(value: String) { text = value }
        }
        return EditableString(prop).apply { undoDepth = 10 } to { text }
    }

    @Test
    fun `substring counts code points rather than UTF-16 units`() {
        val emoji = "\uD83D\uDE00"
        val codePoints = CodePoints("a${emoji}b")

        assertEquals(3, codePoints.size)
        assertEquals(emoji, codePoints.substring(1..1))
        assertEquals("ab", codePoints.substring(0..0) + codePoints.substring(2..2))
    }

    @Test
    fun `undo steps back and redo returns`() {
        val (editable, text) = editable("abc")
        editable.addUndo()
        editable.insert("d")
        editable.addUndo()
        assertEquals("abcd", text())

        editable.undo()
        assertEquals("abc", text())

        editable.redo()
        assertEquals("abcd", text())
    }

    @Test
    fun `undo without history changes nothing`() {
        val (editable, text) = editable("abc")
        editable.addUndo()
        editable.undo()
        assertEquals("abc", text())
    }

    @Test
    fun `top level translation keys have no leading dot`() {
        assertEquals("name", KeyInfo("name", emptyList()).path)
        assertEquals("text", top.spawn("text").path)
        assertEquals("kotlinmcui.utils.true", KeyInfo("true", listOf("kotlinmcui", "utils")).path)
    }
}
