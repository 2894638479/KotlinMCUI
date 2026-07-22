package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslTextBuilderContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.input.Key
import io.github.u2894638479.kotlinmcui.input.KeyModifiers
import io.github.u2894638479.kotlinmcui.input.MouseButton
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.math.rect.expand
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.input.PreeditInfo
import io.github.u2894638479.kotlinmcui.text.AlignableChar
import io.github.u2894638479.kotlinmcui.text.CodePoints
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.text.DslText
import io.github.u2894638479.kotlinmcui.text.DslTextLine
import io.github.u2894638479.kotlinmcui.text.EditableString
import io.github.u2894638479.kotlinmcui.text.kotlinmcui
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// ── Decorator ────────────────────────────────────────────────────────────────

context(ctx: DslContext)
fun DslChild.editBoxBackground(width: Measure = 1.scaled, padding: Measure = width + 1.scaled)
= change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.fillRect(instance.rect.expand(padding),
            if(instance.isHighlighted) Color(255,255,255) else Color(0xA0,0xA0,0xA0))
        backend.fillRect(instance.rect.expand(padding - width),Color.BLACK)
        it.render()
    }

    override val modifier get() = it.modifier.padding(padding)
}}

// ── Internal cursor helpers ──────────────────────────────────────────────────

/** Maps between a character offset in the string and a (line, index) position. */
private class CursorPos(val line: Int, val index: Int)

private fun cursorFromOffset(text: String, offset: Int?): CursorPos? {
    if (offset == null) return null
    var count = 0
    text.lines().forEachIndexed { i, chars ->
        val codePointCount = CodePoints(chars).size
        if (offset > count + codePointCount) {
            count += (codePointCount + 1)
            return@forEachIndexed
        }
        return CursorPos(i, offset - count)
    }
    return null
}

private fun offsetFromCursor(text: String, pos: CursorPos?, maxOffset: Int): Int? {
    if (pos == null) return null
    val lines = text.lines()
    if (pos.line < 0) return 0
    if (pos.line >= lines.size) return maxOffset
    val prevLinesCount = lines.take(pos.line).sumOf { CodePoints(it).size + 1 }
    val lastLineCount = min(CodePoints(lines[pos.line]).size, pos.index)
    return prevLinesCount + lastLineCount
}

private fun cursorRectAt(
    cursorPos: CursorPos?,
    alignedLines: List<Pair<DslTextLine, List<AlignableChar>>>,
    fallbackX: Measure,
    scale: Double
): Rect? {
    if (cursorPos == null) return null
    val (line, chars) = alignedLines.getOrNull(cursorPos.line) ?: return null
    val char = chars.getOrNull(cursorPos.index - 1)
    val x = char?.high ?: chars.firstOrNull()?.low ?: fallbackX
    val half = 0.5.px * scale
    return Rect(x - half, line.low, x + half, line.high)
}

private fun mouseHitPos(
    mouse: Position,
    alignedLines: List<Pair<DslTextLine, List<AlignableChar>>>
): CursorPos {
    if (alignedLines.isEmpty()) return CursorPos(0, 0)
    val selectedLine = alignedLines.minBy { it.first.run { (low + high) / 2 - mouse.y }.absoluteValue }
    val chars = selectedLine.second
    if (chars.isEmpty()) return CursorPos(alignedLines.indexOf(selectedLine), 0)
    val xValues = chars.map { it.low } + chars.last().high
    val idx = xValues.indices.minBy { (xValues[it] - mouse.x).absoluteValue }
    return CursorPos(alignedLines.indexOf(selectedLine), idx)
}

// ── DSL function ─────────────────────────────────────────────────────────────

context(ctx: DslContext)
fun EditableText(
    modifier: Modifier = Modifier,
    stringProp: StableRW<String> = local { "" },
    style: DslCharStyle = DslCharStyle(),
    color: Color = Color.WHITE,
    selectedColor: Color = Color(20, 20, 200),
    size: Measure = 9.scaled,
    fontName: String? = null,
    horizontalAligner: Aligner = Aligner.close(Align.LOW),
    verticalAligner: Aligner = Aligner.close(Align.MID),
    blinkCycle: Duration = 0.5.seconds,
    undoDepth: Int = 10,
    selfRenderPreedit: Boolean = true,
    id: Any
) = context(object : DslIdContext {
    override val identity = newChildId(id)
}) {
    var string by stringProp
    val editable by local { EditableString(stringProp) }
    editable.undoDepth = undoDepth
    val info by local { object {
        var blinkBeginNano = ctx.dataStore.frameBeginNano
        var alignedLines = listOf<Pair<DslTextLine, List<AlignableChar>>>()
        var mouseDown: Unit? = null
    }}
    val font = ctx.dataStore.backend.getFont(fontName)
    val blink = (((ctx.dataStore.frameBeginNano - info.blinkBeginNano) / blinkCycle.inWholeNanoseconds) % 2L) == 0L
    val delegate = DslText(
        identity, modifier, fontName, font, ctx,
        DslTextBuilderContext(ctx).apply { string.emit(color, size, style) }.toChars(),
        size, horizontalAligner, verticalAligner
    )

    info.run {
        collect(object : DslComponent by delegate {
            override val narratable get() = true
            override val narration get() = "${kotlinmcui.narration.editabletext()} $string"
            override val focusable get() = true

            // ── Cursor state ────────────────────────────────────────────

            var cursorPos: CursorPos?
                get() = cursorFromOffset(string, editable.cursor)
                set(value) { editable.cursor = offsetFromCursor(string, value, editable.maxCursor) ?: return }

            fun cursorRect(): Rect? =
                cursorRectAt(cursorPos, alignedLines, instance.rect.left, scale)

            // ── Render ──────────────────────────────────────────────────

            context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
            override fun <RP> render() {
                editable.addUndo()
                val font = backend.getFont(fontName)
                alignedLines = delegate.lines().map { it to it.alignedChars(font) }.apply {
                    val range = editable.range
                    if (instance.isFocused && range != null) {
                        var index = 0
                        forEach { (line, chars) ->
                            chars.forEach {
                                if (index in range) backend.fillRect(Rect(it, line), selectedColor)
                                index++
                            }
                            index++
                        }
                    }
                    backend.flush()
                    forEach { (line, chars) -> line.renderChars(font, chars) }
                    backend.flush()
                }

                if (!instance.isFocused) return
                if (blink) {
                    backend.fillRect(cursorRect() ?: return, color)
                    backend.flush()
                }
            }

            // ── Character input ─────────────────────────────────────────

            override fun charTyped(c: Char): Boolean {
                if (!instance.isFocused) return false
                editable.insert(c)
                return true
            }

            // ── IME ─────────────────────────────────────────────────────

            override val preeditArea get() = cursorRect()
            override val selfRenderPreedit get() = selfRenderPreedit

            override fun globalPreedit(info: PreeditInfo) {
                if (selfRenderPreedit && instance.isFocused) {
                    val text = info.toString()
                    val cursor = editable.cursor2 ?: editable.cursor
                    editable.insert(text)
                    editable.cursor2 = cursor
                }
            }

            // ── Keyboard ────────────────────────────────────────────────

            context(eventModifier: KeyModifiers)
            override fun keyDown(key: Key, scanCode: Int): Boolean {
                fun default() = delegate.keyDown(key, scanCode)
                if (!instance.isFocused) return default()
                editable.check()
                blinkBeginNano = System.nanoTime()

                when (key) {
                    Key.BACKSPACE -> if (!editable.backspace()) return default()
                    Key.DELETE    -> if (!editable.delete()) return default()
                    Key.LEFT      -> if (!moveCursorHorizontal(-1)) return default()
                    Key.RIGHT     -> if (!moveCursorHorizontal(+1)) return default()
                    Key.UP        -> if (!moveCursorVertical(-1)) return default()
                    Key.DOWN      -> if (!moveCursorVertical(+1)) return default()
                    Key.HOME      -> if (!moveCursorToLineStart()) return default()
                    Key.END       -> if (!moveCursorToLineEnd()) return default()
                    Key.ENTER     -> editable.insert('\n')
                    Key.V         -> if (eventModifier.ctrl) editable.insert(ctx.dataStore.backend.clipboard)
                    Key.C         -> if (eventModifier.ctrl) editable.range?.let { ctx.dataStore.backend.clipboard = string.substring(it) }
                    Key.Z         -> if (eventModifier.ctrl) { if (eventModifier.shift) editable.redo() else editable.undo() }
                    Key.X         -> if (eventModifier.ctrl) { editable.range?.let { ctx.dataStore.backend.clipboard = string.substring(it) }; editable.insert() }
                    Key.A         -> if (eventModifier.ctrl) { editable.cursor = editable.maxCursor; editable.cursor2 = 0 }
                    else          -> return default()
                }
                return true
            }

            // ── Keyboard helpers ────────────────────────────────────────

            context(eventModifier: KeyModifiers)
            private fun checkCursor2() {
                if (eventModifier.shift) {
                    if (editable.cursor2 == null) editable.cursor2 = editable.cursor
                } else editable.cursor2 = null
            }

            /** Returns true if the cursor position changed. */
            context(eventModifier: KeyModifiers)
            private fun moveCursorHorizontal(delta: Int): Boolean {
                val origCursor = editable.cursor
                val origCursor2 = editable.cursor2
                checkCursor2()
                if (eventModifier.shift) {
                    if (editable.cursor2 == null) editable.cursor2 = editable.cursor
                    editable.cursor = max(0, editable.cursor + delta)
                } else {
                    editable.cursor = if (delta < 0)
                        min(editable.cursor, editable.cursor2 ?: max(0, editable.cursor + delta))
                    else
                        max(editable.cursor, editable.cursor2 ?: min(editable.maxCursor, editable.cursor + delta))
                    editable.cursor2 = null
                }
                return origCursor != editable.cursor || origCursor2 != editable.cursor2
            }

            /** Returns true if the cursor line changed. */
            context(eventModifier: KeyModifiers)
            private fun moveCursorVertical(delta: Int): Boolean {
                val origPos = cursorPos
                checkCursor2()
                cursorPos = cursorPos?.let { CursorPos(it.line + delta, it.index) }
                return cursorPos != origPos
            }

            /** Returns true if the cursor position changed. */
            context(eventModifier: KeyModifiers)
            private fun moveCursorToLineStart(): Boolean {
                val origPos = cursorPos
                checkCursor2()
                cursorPos = cursorPos?.let { CursorPos(it.line, 0) }
                return cursorPos != origPos
            }

            /** Returns true if the cursor position changed. */
            context(eventModifier: KeyModifiers)
            private fun moveCursorToLineEnd(): Boolean {
                val origPos = cursorPos
                checkCursor2()
                cursorPos = cursorPos?.let { CursorPos(it.line, Int.MAX_VALUE) }
                return cursorPos != origPos
            }

            // ── Mouse ───────────────────────────────────────────────────

            context(eventModifier: KeyModifiers, mouse: Position)
            override fun mouseDown(mouseButton: MouseButton): Boolean {
                if (mouse !in instance.rect) return delegate.mouseDown(mouseButton)
                editable.check()
                blinkBeginNano = System.nanoTime()
                mouseDown = Unit
                cursorPos = mouseHitPos(mouse, alignedLines)
                editable.cursor2 = null
                return true
            }

            context(eventModifier: KeyModifiers, mouse: Position)
            override fun mouseUp(mouseButton: MouseButton): Boolean =
                delegate.mouseUp(mouseButton) || mouseDown == Unit.also { mouseDown = null }

            context(mouse: Position)
            override fun mouseMove() {
                mouseDown ?: return
                editable.check()
                blinkBeginNano = System.nanoTime()
                editable.run { if (cursor2 == null) cursor2 = cursor }
                cursorPos = mouseHitPos(mouse, alignedLines)
                delegate.mouseMove()
            }
        })
    }
}
