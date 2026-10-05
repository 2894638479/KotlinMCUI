package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.math.max
import kotlin.math.min

private class UndoInfo(val codePoints: CodePoints, val cursor: Int, val cursor2: Int?)

/**
 * A mutable string model for editable text components.
 *
 * Manages cursor position, text selection, insertion, deletion, and
 * undo/redo history. The underlying string is stored in a [StableRW]
 * property, with [CodePoints] used for Unicode-aware editing operations.
 */
internal class EditableString(private val stringProp: StableRW<String>) {
    var codePoints get() = CodePoints(stringProp.value)
        set(value) { stringProp.value = value.string }
    var cursor = codePoints.size
    var cursor2: Int? = null
    var undoDepth = 0
    private val undoList = mutableListOf<UndoInfo>()
    private var redo: UndoInfo? = null

    // ── Editing ──────────────────────────────────────────────────────────

    /** Replace the selection range (or insert at cursor) with [value]. */
    fun insert(value: IntArray = IntArray(0)) {
        check()
        val max = max(cursor, cursor2 ?: cursor)
        val min = min(cursor, cursor2 ?: cursor)
        val countToEnd = codePoints.size - max
        codePoints = codePoints.replace(min, max, value)
        cursor = codePoints.size - countToEnd
        cursor2 = null
    }

    fun insert(c: Char) = insert(IntArray(1) { c.code })
    fun insert(s: String) = insert(CodePoints(s).arr)

    /** Delete the character after the cursor (or delete the selection).
     * @return true if text was modified. */
    fun delete(): Boolean {
        check()
        if (cursor2 != null) { insert(); return true }
        if (cursor == codePoints.size) return false
        codePoints = codePoints.replace(cursor..cursor, IntArray(0))
        return true
    }

    /** Delete the character before the cursor (or delete the selection).
     * @return true if text was modified. */
    fun backspace(): Boolean {
        check()
        if (cursor2 != null) { insert(); return true }
        if (cursor == 0) return false
        val countToEnd = codePoints.size - max(cursor, cursor2 ?: cursor)
        codePoints = codePoints.replace(cursor - 1..cursor - 1, IntArray(0))
        cursor = codePoints.size - countToEnd
        return true
    }

    // ── Cursor & selection ──────────────────────────────────────────────

    /** The selected range, or null if nothing is selected. */
    val range get() = cursor2?.let { min(it, cursor)..<max(it, cursor) }
    inline val maxCursor get() = codePoints.size

    /** Bring cursor state into sync with the backing string. */
    fun check() {
        codePoints = CodePoints(stringProp.value)
        val indices = 0..codePoints.size
        cursor = cursor.coerceIn(indices)
        cursor2?.let { cursor2 = it.coerceIn(indices) }
    }

    // ── Undo / Redo ─────────────────────────────────────────────────────

    private var undo get() = UndoInfo(codePoints, cursor, cursor2)
        set(value) {
            redo = undo
            codePoints = value.codePoints
            cursor = value.cursor
            cursor2 = value.cursor2
        }

    fun addUndo() {
        check()
        val list = undoList
        if (undoDepth <= 0) return list.clear()
        if (list.firstOrNull()?.codePoints?.contentEquals(codePoints) == true) {
            list[0] = undo
            return
        }
        list.add(0, undo)
        if (list.size > undoDepth) list.removeAt(list.size - 1)
        redo = null
    }

    fun redo() {
        check()
        redo?.let {
            undo = it
            redo = null
        }
    }

    /**
     * Step back one entry.
     *
     * Maintains the invariant that [undoList] starts with the current state: the
     * entry being left is dropped, and [redo] is filled in by the [undo] setter.
     */
    fun undo() {
        check()
        val previous = undoList.getOrNull(1) ?: return
        undoList.removeAt(0)
        undo = previous
    }
}
