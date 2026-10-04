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

    // ── delete(): removes the character *after* the cursor ─────────────────

    @Test
    fun `delete removes the character after the cursor`() {
        val (editable, text) = editable("abcd")
        editable.cursor = 1
        assertTrue(editable.delete(), "deleting a character must report a modification")
        assertEquals("acd", text(), "delete removes 'b', the character after the cursor")
        assertEquals(1, editable.cursor, "delete must not move the cursor")
    }

    @Test
    fun `delete removes the first character when the cursor is at zero`() {
        val (editable, text) = editable("abc")
        editable.cursor = 0
        assertTrue(editable.delete())
        assertEquals("bc", text())
    }

    @Test
    fun `delete removes the last character when the cursor is one before the end`() {
        val (editable, text) = editable("abc")
        editable.cursor = 2
        assertTrue(editable.delete())
        assertEquals("ab", text())
        assertEquals(2, editable.cursor)
    }

    @Test
    fun `delete at the end of the text reports no modification`() {
        val (editable, text) = editable("abc")
        assertFalse(editable.delete(), "the cursor is already past the last character")
        assertEquals("abc", text())
    }

    @Test
    fun `delete with a selection removes the selection`() {
        val (editable, text) = editable("abcdef")
        editable.cursor = 1
        editable.cursor2 = 4
        assertTrue(editable.delete())
        assertEquals("aef", text(), "the selection 1..4 must go, not the character after the cursor")
        assertNull(editable.cursor2, "deleting the selection must clear it")
    }

    // ── backspace(): removes the character *before* the cursor ─────────────

    @Test
    fun `backspace removes the character before the cursor`() {
        val (editable, text) = editable("abcd")
        editable.cursor = 2
        assertTrue(editable.backspace(), "backspacing a character must report a modification")
        assertEquals("acd", text(), "backspace removes 'b', the character before the cursor")
        assertEquals(1, editable.cursor, "the cursor must follow the deleted character")
    }

    @Test
    fun `backspace removes the last character when the cursor is at the end`() {
        val (editable, text) = editable("abc")
        assertTrue(editable.backspace())
        assertEquals("ab", text())
        assertEquals(2, editable.cursor)
    }

    @Test
    fun `backspace removes the first character when the cursor is one`() {
        val (editable, text) = editable("abc")
        editable.cursor = 1
        assertTrue(editable.backspace())
        assertEquals("bc", text())
        assertEquals(0, editable.cursor)
    }

    @Test
    fun `backspace at the start of the text reports no modification`() {
        val (editable, text) = editable("abc")
        editable.cursor = 0
        assertFalse(editable.backspace(), "there is no character before the cursor")
        assertEquals("abc", text())
    }

    @Test
    fun `backspace with a reversed selection removes the selection`() {
        val (editable, text) = editable("abcdef")
        editable.cursor = 4
        editable.cursor2 = 1
        assertTrue(editable.backspace())
        assertEquals("aef", text(), "the selection must go even when it is written backwards")
        assertNull(editable.cursor2)
    }
}
