package io.github.u2894638479.kotlinmcui.text

/**
 * A mutable sequence of Unicode code points backed by an [IntArray].
 *
 * Provides efficient insert, replace, and slice operations for text editing.
 * The [Const] inner class is an immutable variant used by [io.github.u2894638479.kotlinmcui.input.PreeditInfo].
 */
@JvmInline
value class CodePoints(internal val arr: IntArray) {
    /** An immutable view of a code point array. */
    @JvmInline
    value class Const(private val arr: IntArray) {
        constructor(str: String) : this(str.codePoints().toArray())
        operator fun get(index: Int) = arr[index]
        operator fun get(range: IntRange) = arr.sliceArray(range)
        val string get() = String(arr, 0, arr.size)
        val size get() = arr.size
        infix fun contentEquals(other: CodePoints) = arr.contentEquals(other.arr)
    }

    constructor(str: String) : this(str.codePoints().toArray())
    operator fun get(index: Int) = arr[index]
    operator fun get(range: IntRange) = arr.sliceArray(range)
    val string get() = String(arr, 0, arr.size)
    val size get() = arr.size

    /** Replace the range [[first], [end]) with [value]. */
    fun replace(first: Int, end: Int, value: IntArray): CodePoints {
        val arr0 = arr.sliceArray(0..<first)
        val arr1 = arr.sliceArray(end..<size)
        return CodePoints(arr0 + value + arr1)
    }

    /** Replace [range] with [value]. */
    fun replace(range: IntRange, value: IntArray): CodePoints {
        require(!range.isEmpty())
        return replace(range.first, range.last + 1, value)
    }

    /** The text covered by [range]. Unlike [String.substring] the range counts code points. */
    fun substring(range: IntRange): String {
        val slice = arr.sliceArray(range)
        return String(slice, 0, slice.size)
    }

    /** Insert [value] at [index]. */
    fun insert(index: Int, value: IntArray) = replace(index, index, value)

    infix fun contentEquals(other: CodePoints) = arr.contentEquals(other.arr)
}
