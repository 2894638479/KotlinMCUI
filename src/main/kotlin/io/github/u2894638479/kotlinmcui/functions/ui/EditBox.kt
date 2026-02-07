package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.isFocused
import io.github.u2894638479.kotlinmcui.component.isHighlighted
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.context.DslTextBuilderContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.*
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
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.text.AlignableChar
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.text.DslText
import io.github.u2894638479.kotlinmcui.text.DslTextLine
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

context(ctx: DslContext)
fun DslChild.editBoxBackground(width: Measure = 1.scaled, padding: Measure = width + 1.scaled)
= change { object: DslComponent by it {
    context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        backend.fillRect(instance.rect.expand(padding),
            if(isHighlighted) Color(255,255,255) else Color(0xA0,0xA0,0xA0))
        backend.fillRect(instance.rect.expand(padding - width),Color.BLACK)
        it.render(mouse)
    }

    override val modifier get() = it.modifier.padding(padding)
}}

@JvmInline
private value class CodePoints(val arr: IntArray) {
    constructor(str:String):this(str.codePoints().toArray())
    operator fun get(index: Int) = arr[index]
    operator fun get(range: IntRange) = arr.sliceArray(range)
    val string get() = String(arr,0,arr.size)
    val size get() = arr.size
    // [first,end)
    fun replace(first: Int, end: Int, value: IntArray): CodePoints {
        val arr0 = arr.sliceArray(0..<first)
        val arr1 = arr.sliceArray(end..<size)
        return CodePoints(arr0 + value + arr1)
    }
    fun replace(range: IntRange, value: IntArray): CodePoints {
        require(!range.isEmpty())
        return replace(range.first,range.last + 1,value)
    }
    fun insert(index: Int, value: IntArray) = replace(index,index,value)
    fun contentEquals(other: CodePoints) = arr.contentEquals(other.arr)
}

private class UndoInfo(val codePoints: CodePoints, val cursor: Int, val cursor2: Int?)

private class EditableString(private val stringProp: StableRWProperty<String>) {
    var codePoints get() = CodePoints(stringProp.value)
        set(value) { stringProp.value = value.string }
    var cursor = codePoints.size
    var cursor2:Int? = null
    var undoDepth = 0
    val undoList = mutableListOf<UndoInfo>()
    var redo: UndoInfo? = null

    fun insert(value: IntArray = IntArray(0)) {
        check()
        val max = max(cursor,cursor2?:cursor)
        val min = min(cursor,cursor2?:cursor)
        val countToEnd = codePoints.size - max
        codePoints = codePoints.replace(min,max,value)
        cursor = codePoints.size - countToEnd
        cursor2 = null
    }
    fun insert(c:Char) = insert(IntArray(1){ c.code })
    fun insert(s:String) = insert(CodePoints(s).arr)

    fun delete() {
        check()
        if(cursor2 != null) return insert()
        if(cursor == codePoints.size) return
        codePoints = codePoints.replace(cursor..cursor, IntArray(0))
    }

    fun backspace() {
        check()
        if(cursor2 != null) return insert()
        if(cursor == 0) return
        val countToEnd = codePoints.size - max(cursor, cursor2 ?: cursor)
        codePoints = codePoints.replace(cursor-1..cursor-1, IntArray(0))
        cursor = codePoints.size - countToEnd
    }

    private var undo get() = UndoInfo(codePoints,cursor,cursor2)
        set(value) {
            redo = undo
            codePoints = value.codePoints
            cursor = value.cursor
            cursor2 = value.cursor2
        }

    fun check() {
        codePoints = CodePoints(stringProp.value)
        val indices = 0..codePoints.size
        cursor = cursor.coerceIn(indices)
        cursor2?.let { cursor2 = it.coerceIn(indices) }
    }

    inline fun ifCursorUnChanged(action:()->Unit,block: EditableString.()->Unit) {
        val origCursor = cursor
        val origCursor2 = cursor2
        block()
        if(origCursor == cursor && origCursor2 == cursor2) action()
    }

    inline fun ifStringUnChanged(action:()->Unit,block: EditableString.()->Unit) {
        val origStr = codePoints
        block()
        if(origStr == codePoints) action()
    }

    inline fun ifNothingChanged(action: () -> Unit,block: EditableString.() -> Unit) {
        var changed = false
        ifCursorUnChanged({changed = true}) {
            ifStringUnChanged({changed = true},block)
        }
        if(changed) action()
    }

    fun addUndo() {
        check()
        val list = undoList
        if(undoDepth <= 0) return list.clear()
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

    fun undo() {
        check()
        redo = undoList.removeFirstOrNull()
        undo = undoList.firstOrNull() ?: return
    }

    val range get() = cursor2?.let {
        min(it,cursor)..<max(it,cursor)
    }
    inline val maxCursor get() = codePoints.size
}

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
) = context(object : DslIdContext {
    override val identity = newChildId(id)
}) {
    val stringProp = string ?: run {
        val prop by remember("").property
        prop
    }
    var string by stringProp
    val editable by remember { EditableString(stringProp) }
    editable.undoDepth = undoDepth
    val info by remember {object{
        var blinkBeginNano = ctx.dataStore.frameTimeNano
        var alignedLines = listOf<Pair<DslTextLine, List<AlignableChar>>>()
        var mouseDown: Unit? = null
    }}
    val font = ctx.dataStore.backend.getFont(fontName)
    val blink = (((ctx.dataStore.frameTimeNano - info.blinkBeginNano) / blinkCycle.inWholeNanoseconds) % 2L) == 0L
    class CursorPos(val line:Int,val index:Int)
    val delegate = DslText(
        identity, modifier, fontName, font,
        DslTextBuilderContext(ctx).apply { string.emit(color, size, style) }.toChars(),
        size, horizontalAligner, verticalAligner
    )
    info.run {
        collect(object : DslComponent by delegate {

            context(instance: DslComponent)
            override val focusable get() = true

            fun getPos(cursor: Int?): CursorPos? {
                var count = 0
                val cursor = cursor ?: return null
                string.lines().forEachIndexed { i, chars ->
                    val codePointCount = CodePoints(chars).size
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
                if (value.line >= lines.size) return editable.maxCursor
                if (value.line !in lines.indices) return null
                val prevLinesCount = lines.take(value.line).sumOf { CodePoints(it).size + 1 }
                val lastLineCount = min(CodePoints(lines[value.line]).size, value.index)
                return prevLinesCount + lastLineCount
            }

            var cursorPos: CursorPos?
                get() = getPos(editable.cursor)
                set(value) { editable.cursor = setPos(value) ?: return }

            context(instance: DslComponent)
            fun cursorRect(): Rect? {
                val cursorPos = cursorPos ?: return null
                val (line, chars) = alignedLines.getOrNull(cursorPos.line) ?: return null
                val char = chars.getOrNull(cursorPos.index - 1)
                val x = char?.high ?: chars.firstOrNull()?.low ?: instance.rect.left
                return Rect(x - 0.5.scaled, line.low, x + 0.5.scaled, line.high)
            }

            context(backend: DslBackendRenderer<RP>, renderParam: RP, instance: DslComponent)
            override fun <RP> render(mouse: Position) {
                editable.addUndo()
                val font = backend.getFont(fontName)
                alignedLines = delegate.lines().map { it to it.alignedChars(font) }.apply {
                    val range = editable.range
                    if (isFocused && range != null) {
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
                editable.insert(c)
                return true
            }

            context(instance: DslComponent)
            override fun keyDown(key: Int, scanCode: Int, eventModifier: EventModifier): Boolean {
                fun default() = delegate.keyDown(key, scanCode, eventModifier)
                if (!isFocused) return default()
                editable.check()
                blinkBeginNano = System.nanoTime()
                fun EditableString.checkCursor2() {
                    if (eventModifier.shift) {
                        if (cursor2 == null) cursor2 = cursor
                    } else cursor2 = null
                }
                when (key) {
                    GLFW.GLFW_KEY_BACKSPACE -> editable.ifNothingChanged({ return default() }) {
                        backspace()
                    }

                    GLFW.GLFW_KEY_DELETE -> editable.ifNothingChanged({ return default() }) {
                        delete()
                    }

                    GLFW.GLFW_KEY_LEFT -> editable.ifCursorUnChanged({ return default() }) {
                        if (eventModifier.shift) {
                            if (cursor2 == null) cursor2 = cursor
                            cursor = max(0, cursor - 1)
                        } else {
                            cursor = min(cursor, cursor2 ?: (max(0, cursor - 1)))
                            cursor2 = null
                        }
                    }

                    GLFW.GLFW_KEY_RIGHT -> editable.ifCursorUnChanged({ return default() }) {
                        if (eventModifier.shift) {
                            if (cursor2 == null) cursor2 = cursor
                            cursor = min(maxCursor, cursor + 1)
                        } else {
                            cursor = max(cursor, cursor2 ?: (min(maxCursor, cursor + 1)))
                            cursor2 = null
                        }
                    }

                    GLFW.GLFW_KEY_UP -> editable.ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line - 1, it.index) }
                    }

                    GLFW.GLFW_KEY_DOWN -> editable.ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line + 1, it.index) }
                    }

                    GLFW.GLFW_KEY_HOME -> editable.ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line, 0) }
                    }

                    GLFW.GLFW_KEY_END -> editable.ifCursorUnChanged({ return default() }) {
                        checkCursor2()
                        cursorPos = cursorPos?.let { CursorPos(it.line, Int.MAX_VALUE) }
                    }

                    GLFW.GLFW_KEY_ENTER -> editable.insert('\n')
                    GLFW.GLFW_KEY_V -> if (eventModifier.ctrl) editable.insert(ctx.dataStore.backend.clipBoard)
                    GLFW.GLFW_KEY_C -> if (eventModifier.ctrl) editable.range?.let {
                        ctx.dataStore.backend.clipBoard = string.substring(it)
                    }

                    GLFW.GLFW_KEY_Z -> if (eventModifier.ctrl) {
                        if (eventModifier.shift) editable.redo() else editable.undo()
                    }

                    GLFW.GLFW_KEY_X -> if (eventModifier.ctrl) {
                        editable.range?.let { ctx.dataStore.backend.clipBoard = string.substring(it) }
                        editable.insert()
                    }

                    GLFW.GLFW_KEY_A -> if (eventModifier.ctrl) {
                        editable.cursor = editable.maxCursor
                        editable.cursor2 = 0
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
                editable.check()
                blinkBeginNano = System.nanoTime()
                mouseDown = Unit
                cursorPos = mouseHitPos(mouse)
                editable.cursor2 = null
                return true
            }

            context(instance: DslComponent)
            override fun mouseUp(mouse: Position, mouseButton: MouseButton): Boolean {
                return delegate.mouseUp(mouse, mouseButton) || mouseDown == Unit.also { mouseDown = null }
            }

            context(instance: DslComponent)
            override fun mouseMove(mouse: Position) {
                mouseDown ?: return
                editable.check()
                blinkBeginNano = System.nanoTime()
                editable.run { if (cursor2 == null) cursor2 = cursor }
                cursorPos = mouseHitPos(mouse)
                delegate.mouseMove(mouse)
            }
        })
    }
}
