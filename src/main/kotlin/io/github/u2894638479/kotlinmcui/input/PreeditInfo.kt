package io.github.u2894638479.kotlinmcui.input

import io.github.u2894638479.kotlinmcui.text.CodePoints

/**
 * IME preedit (composition) state received from the platform.
 *
 * While the user is composing text (e.g. Pinyin for Chinese characters), the OS
 * IME sends partial text grouped into [blocks], with one block marked as
 * [focusedBlock] (the active candidate) and a [caret] position within the
 * composed string.
 *
 * @param blocks The logical blocks of the composition string. Each block may
 *               represent a different segment (e.g. raw input vs. candidate text).
 * @param focusedBlock Index into [blocks] of the currently active block.
 * @param caret Cursor position within the composed text.
 */
class PreeditInfo(
    val blocks: List<CodePoints.Const>,
    val focusedBlock: Int,
    val caret: Int,
) {
    val isEmpty get() = blocks.isEmpty()
    override fun toString() = blocks.joinToString("\'") { it.string }

    companion object {
        /** Sentinel representing no active composition. */
        val empty get() = PreeditInfo(emptyList(), 0, 0)
    }
}
