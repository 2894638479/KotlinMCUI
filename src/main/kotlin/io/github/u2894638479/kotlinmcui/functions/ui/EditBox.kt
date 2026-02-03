package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslTextBuilderContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.glfw.EventModifier
import io.github.u2894638479.kotlinmcui.glfw.MouseButton
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.prop.StableRWProperty
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.text.AlignableChar
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.text.DslText
import io.github.u2894638479.kotlinmcui.text.DslTextLine
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import org.lwjgl.glfw.GLFW
import kotlin.collections.plus
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

context(ctx: DslContext)
fun DslChild.editBoxBackground(color: Color = Color.WHITE) = change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.renderEditBox(instance.rect, isHighlighted,color)
        it.render(mouse)
    }

    override val modifier get() = it.modifier.padding(1.scaled)
}}

context(ctx: DslContext)
fun EditableText(
    modifier: Modifier = Modifier,
    string: StableRWProperty<String>? = null,
    style: DslCharStyle = DslCharStyle(),
    color: Color = Color.WHITE,
    selectedColor: Color = Color(20, 20, 200),
    size: Measure =  9.scaled,
    fontName: String? = null,
    horizontalAligner: Aligner = Aligner.close(Align.LOW),
    verticalAligner: Aligner = Aligner.close(Align.MID),
    blinkCycle: Duration = 0.5.seconds,
    undoDepth:Int = 10,
    id:Any
) = run {
    val id = newChildId(id)
    var string by string ?: run {
        val prop by dataStore.remember(id,"").property
        prop
    }
    class UndoInfo(val string: String, val cursor: Int, val cursor2: Int?)
    val info by dataStore.remember(id) {object{
        var cursor = string.codePoints().count().toInt()
        var cursor2: Int? = null
        var blinkBeginNano = ctx.dataStore.frameTimeNano
        var alignedLines = listOf<Pair<DslTextLine, List<AlignableChar>>>()
        val undoList = mutableListOf<UndoInfo>()
        var redo: UndoInfo? = null
        var mouseDown: Unit? = null
        fun check() {
            val indices = 0..string.length
            if(indices.isEmpty()) return
            cursor = cursor.coerceIn(indices)
            cursor2?.let { cursor2 = it.coerceIn(indices) }
        }
        inline fun ifCursorUnChanged(action:()->Unit,block:()->Unit) {
            val origCursor = cursor
            val origCursor2 = cursor2
            block()
            if(origCursor == cursor && origCursor2 == cursor2) action()
        }
        inline fun ifStringUnChanged(action:()->Unit,block:()->Unit) {
            val origStr = string
            block()
            if(origStr == string) action()
        }
    }}
    val font = ctx.dataStore.backend.getFont(fontName)
    val blink = (((ctx.dataStore.frameTimeNano - info.blinkBeginNano) / blinkCycle.inWholeNanoseconds) % 2L) == 0L
    class CursorPos(val line:Int,val index:Int)
    val delegate = DslText(
        id, modifier, fontName, font,
        DslTextBuilderContext(ctx).apply { string.emit(color, size, style) }.toChars(),
        size, horizontalAligner, verticalAligner
    )
    info.run {
        collect(object : DslComponent by delegate {
            inline val String.codeCount get() = codePoints().count().toInt()
            inline val maxCursor get() = string.codeCount

            context(instance: DslComponent)
            override val focusable get() = true

            fun addUndo() {
                check()
                val list = undoList
                if (list.firstOrNull()?.string == string) {
                    list[0] = UndoInfo(string, cursor, cursor2)
                    return
                }
                list.add(0, UndoInfo(string, cursor, cursor2))
                if (list.size > undoDepth) list.removeAt(list.size - 1)
                redo = null
            }

            fun undo() {
                check()
                redo = undoList.removeFirstOrNull()
                undoList.firstOrNull()?.let {
                    string = it.string
                    cursor = it.cursor
                    cursor2 = it.cursor2
                }
            }

            fun redo() {
                check()
                redo?.let {
                    string = it.string
                    cursor = it.cursor
                    cursor2 = it.cursor2
                    redo = null
                }
            }

            fun getPos(cursor: Int?): CursorPos? {
                var count = 0
                val cursor = cursor ?: return null
                string.lines().forEachIndexed { i, chars ->
                    val codePointCount = chars.codePoints().count().toInt()
                    if (cursor > count + codePointCount) {
                        count += (codePointCount + 1)
                        return@forEachIndexed
                    }
                    return CursorPos(i, cursor - count)
                }
                return null
            }

            fun setPos(value: CursorPos?): Int? {
                if (value == null) return null
                val lines = string.lines()
                if (value.line < 0) return 0
                if (value.line >= lines.size) return maxCursor
                if (value.line !in lines.indices) return null
                val prevLinesCount = lines.take(value.line).sumOf { it.codeCount + 1 }
                val lastLineCount = min(lines[value.line].codeCount, value.index)
                return prevLinesCount + lastLineCount
            }

            var cursorPos: CursorPos?
                get() = getPos(cursor)
                set(value) {
                    cursor = setPos(value) ?: return
                }
            var cursor2Pos: CursorPos?
                get() = getPos(cursor2)
                set(value) {
                    cursor2 = setPos(value) ?: return
                }

            context(instance: DslComponent)
            fun cursorRect(): Rect? {
                val cursorPos = cursorPos ?: return null
                val (line, chars) = alignedLines.getOrNull(cursorPos.line) ?: return null
                val char = chars.getOrNull(cursorPos.index - 1)
                val x = char?.high ?: chars.firstOrNull()?.low ?: instance.rect.left
                return Rect(x - 0.5.scaled, line.low, x + 0.5.scaled, line.high)
            }

            inline val hasRange get() = cursor2 != null && cursor != cursor2
            inline val range: IntRange?
                get() {
                    val c1 = cursor
                    val c2 = cursor2 ?: return null
                    if (c1 == c2) return null
                    return if (c1 < c2) c1..<c2 else c2..<c1
                }

            fun delRange() {
                if (!hasRange) return
                val lastCursor2 = cursor2 ?: return
                addUndo()
                string = string.let {
                    val index2 = it.offsetByCodePoints(0, lastCursor2)
                    val index = it.offsetByCodePoints(0, cursor)
                    if (index > index2) it.substring(0, index2) + it.substring(index)
                    else it.substring(0, index) + it.substring(index2)
                }
                cursor = min(cursor, lastCursor2)
                cursor2 = null
            }

            fun insertChar(c: Char) {
                addUndo()
                if (c == '\r') return
                string = string.let {
                    val index = it.offsetByCodePoints(0, cursor++)
                    cursor2?.let { cursor2 ->
                        info.cursor2 = null
                        val index2 = it.offsetByCodePoints(0, cursor2)
                        if (index > index2) it.substring(0, index2) + c + it.substring(index)
                        else it.substring(0, index) + c + it.substring(index2)
                    } ?: (it.substring(0, index) + c + it.substring(index))
                }
            }

            fun insertString(s: String) {
                addUndo()
                val s = s.filter { it != '\r' }
                string = string.let {
                    val index = it.offsetByCodePoints(0, cursor)
                    cursor += s.codeCount
                    cursor2?.let { cursor2 ->
                        info.cursor2 = null
                        val index2 = it.offsetByCodePoints(0, cursor2)
                        if (index > index2) it.substring(0, index2) + s + it.substring(index)
                        else it.substring(0, index) + s + it.substring(index2)
                    } ?: (it.substring(0, index) + s + it.substring(index))
                }
            }

            fun backChar() {
                addUndo()
                if (hasRange) return delRange()
                if (cursor == 0) return
                string = string.let {
                    val index = it.offsetByCodePoints(0, cursor--)
                    val index1 = it.offsetByCodePoints(0, cursor)
                    it.substring(0, index1) + it.substring(index)
                }
            }

            fun delChar() {
                addUndo()
                if (hasRange) return delRange()
                if (cursor == maxCursor) return
                string = string.let {
                    val cursor = cursor
                    it.substring(0, it.offsetByCodePoints(0, cursor)) + it.substring(
                        it.offsetByCodePoints(
                            0,
                            cursor + 1
                        )
                    )
                }
            }

            context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
            override fun <RP> render(mouse: Position) {
                addUndo()
                val font = backend.getFont(fontName)
                alignedLines = delegate.lines().map { it to it.alignedChars(font) }.apply {
                    if (isFocused) range?.let { range ->
                        var index = 0
                        forEach { (line, chars) ->
                            chars.forEach {
                                if (index in range) backend.fillRect(
                                    Rect(it.low, line.low, it.high, line.high),
                                    selectedColor
                                )
                                index++
                            }
                            index++
                        }
                    }
                    forEach { (line, chars) -> line.renderChars(font, chars) }
                }

                if (!isFocused) return
                if (blink) backend.fillRect(cursorRect() ?: return, color)
            }

            context(instance: DslComponent)
            override fun charTyped(c: Char, eventModifier: EventModifier): Boolean {
                if (!isFocused) return false
                check()
                insertChar(c)
                return true
            }

            context(instance: DslComponent)
            override fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier): Boolean {
                fun default() = delegate.keyDown(key, scanCode, eventModifier)
                if (!isFocused) return default()
                check()
                blinkBeginNano = System.nanoTime()
                fun checkCursor2() {
                    if (eventModifier.shift) {
                        if (cursor2 == null) cursor2 = cursor
                    } else cursor2 = null
                }
                when (key) {
                    GLFW.GLFW_KEY_BACKSPACE -> ifStringUnChanged({ return default() }) {
                        backChar()
                    }

                    GLFW.GLFW_KEY_DELETE -> ifStringUnChanged({ return default() }) {
                        delChar()
                    }

                    GLFW.GLFW_KEY_LEFT -> ifCursorUnChanged({ return default() }) {
                        if (eventModifier.shift) {
                            if (cursor2 == null) cursor2 = cursor
                            cursor = max(0, cursor - 1)
                        } else {
                            cursor = min(cursor, cursor2 ?: (max(0, cursor - 1)))
                            cursor2 = null
                        }
                    }

                    GLFW.GLFW_KEY_RIGHT -> ifCursorUnChanged({ return default() }) {
                        if (eventModifier.shift) {
                            if (cursor2 == null) cursor2 = cursor
                            cursor = min(maxCursor, cursor + 1)
                        } else {
                            cursor = max(cursor, cursor2 ?: (min(maxCursor, cursor + 1)))
                            cursor2 = null
                        }
                    }

                    GLFW.GLFW_KEY_UP -> ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line - 1, it.index) }
                    }

                    GLFW.GLFW_KEY_DOWN -> ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line + 1, it.index) }
                    }

                    GLFW.GLFW_KEY_HOME -> ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line, 0) }
                    }

                    GLFW.GLFW_KEY_END -> ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line, Int.MAX_VALUE) }
                    }

                    GLFW.GLFW_KEY_ENTER -> insertChar('\n')
                    GLFW.GLFW_KEY_V -> if (eventModifier.ctrl) insertString(ctx.dataStore.backend.clipBoard)
                    GLFW.GLFW_KEY_C -> if (eventModifier.ctrl) range?.let {
                        ctx.dataStore.backend.clipBoard = string.substring(it)
                    }

                    GLFW.GLFW_KEY_Z -> if (eventModifier.ctrl) {
                        if (eventModifier.shift) redo() else undo()
                    }

                    GLFW.GLFW_KEY_X -> if (eventModifier.ctrl) {
                        range?.let { ctx.dataStore.backend.clipBoard = string.substring(it) }
                        delRange()
                    }

                    GLFW.GLFW_KEY_A -> if (eventModifier.ctrl) {
                        cursor = maxCursor
                        cursor2 = 0
                    }

                    else -> return default()
                }
                return true
            }

            context(instance: DslComponent)
            override fun keyUp(key: Int, scanCode: Int, eventModifier: EventModifier): Boolean {
                return delegate.keyUp(key, scanCode, eventModifier)
            }

            fun mouseHitPos(mouse: Position): CursorPos {
                val alignedLines = alignedLines
                val selectedLine = alignedLines.ifEmpty { return CursorPos(0, 0) }.minBy {
                    it.first.run { (low + high) / 2 - mouse.y }.absoluteValue
                }
                selectedLine.second.ifEmpty { return CursorPos(alignedLines.indexOf(selectedLine), 0) }
                val xValues = selectedLine.second.map { it.low } + selectedLine.second.last().high
                val selected = xValues.minBy { (it - mouse.x).absoluteValue }
                return CursorPos(alignedLines.indexOf(selectedLine), xValues.indexOf(selected))
            }

            context(instance: DslComponent)
            override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
                if (mouse !in instance.rect) return delegate.mouseDown(mouse, mouseButton)
                check()
                blinkBeginNano = System.nanoTime()
                mouseDown = Unit
                cursorPos = mouseHitPos(mouse)
                cursor2 = null
                return true
            }

            context(instance: DslComponent)
            override fun mouseUp(mouse: Position, mouseButton: MouseButton): Boolean {
                return delegate.mouseUp(mouse, mouseButton) || mouseDown == Unit.also { mouseDown = null }
            }

            context(instance: DslComponent)
            override fun mouseMove(mouse: Position) {
                mouseDown ?: return
                check()
                blinkBeginNano = System.nanoTime()
                if (cursor2 == null) cursor2 = cursor
                cursorPos = mouseHitPos(mouse)
                delegate.mouseMove(mouse)
            }
        })
    }
}
