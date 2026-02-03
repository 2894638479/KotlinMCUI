package io.github.u2894638479.kotlinmcui.math

import kotlin.experimental.ExperimentalTypeInference

@JvmInline
value class MeasureArray private constructor(val raw: DoubleArray) {
    constructor(size: Int):this(DoubleArray(size))
    constructor(size: Int,init:(Int)-> Measure):this(DoubleArray(size){ init(it).raw })
    operator fun get(index:Int) = Measure.ofRaw(raw[index])
    operator fun set(index:Int,value: Measure) = raw.set(index,value.raw)
    @JvmInline
    value class Iterator(private val doubleIterator: DoubleIterator): kotlin.collections.Iterator<Measure> {
        override fun next() = doubleIterator.next().px
        override fun hasNext() = doubleIterator.hasNext()
    }
    operator fun iterator(): kotlin.collections.Iterator<Measure> = Iterator(raw.iterator())
    val size get() = raw.size
    fun isEmpty() = raw.isEmpty()
    fun isNotEmpty() = raw.isNotEmpty()
    fun clone() = MeasureArray(raw.clone())
    fun sum() = raw.sum().px
    fun average() = raw.average().px
    fun first() = raw.first()
    fun last() = raw.last()
    fun firstOrNull() = raw.firstOrNull()
    fun lastOrNull() = raw.lastOrNull()
    inline fun first(predicate:(Measure) -> Boolean) = raw.first { predicate(Measure.ofRaw(it)) }
    inline fun last(predicate:(Measure) -> Boolean) = raw.last { predicate(Measure.ofRaw(it)) }
    inline fun firstOrNull(predicate: (Measure) -> Boolean) = raw.firstOrNull { predicate(Measure.ofRaw(it)) }
    inline fun lastOrNull(predicate: (Measure) -> Boolean) = raw.lastOrNull { predicate(Measure.ofRaw(it)) }
    inline fun <R> map(transform: (Measure) -> R) = raw.map { transform(Measure.ofRaw(it)) }
    inline fun forEach(action:(Measure)-> Unit) = raw.forEach { action(it.px) }
    fun getOrNull(index:Int) = raw.getOrNull(index)?.px
    fun getOrElse(index:Int,defaultValue:(Int)-> Measure) = raw.getOrElse(index) { defaultValue(it).raw }
    val lastIndex get() = raw.lastIndex
    val indices get() = raw.indices
    fun fill(element: Measure, fromIndex: Int = 0, toIndex: Int = size) = raw.fill(element.raw,fromIndex,toIndex)
    fun asSequence() = if(isEmpty()) emptySequence() else Sequence { iterator() }
    fun any(predicate: (Measure) -> Boolean) = raw.any { predicate(Measure.ofRaw(it)) }
    fun asList(): List<Measure> = object : AbstractList<Measure>(), RandomAccess {
        override val size get() = this@MeasureArray.size
        override fun get(index: Int) = this@MeasureArray[index]
        override fun isEmpty() = this@MeasureArray.isEmpty()
    }
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfDouble")
inline fun MeasureArray.sumOf(selector: (Measure) -> Double) = raw.sumOf { selector(it.px) }
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfInt")
inline fun MeasureArray.sumOf(selector: (Measure) -> Int) = raw.sumOf { selector(it.px) }
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfUInt")
inline fun MeasureArray.sumOf(selector: (Measure) -> UInt) = raw.sumOf { selector(it.px) }
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfLong")
inline fun MeasureArray.sumOf(selector: (Measure) -> Long) = raw.sumOf { selector(it.px) }