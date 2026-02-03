package io.github.u2894638479.kotlinmcui.modifier

import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Alignment
import io.github.u2894638479.kotlinmcui.math.px

interface Modifier {
    val width get() = Measure.AUTO
    val height get() = Measure.AUTO
    val weight get() = 1.0
    val paddingTop get() = 0.px
    val paddingLeft get() = 0.px
    val paddingBottom get() = 0.px
    val paddingRight get() = 0.px
    val minWidth get() = 0.px
    val minHeight get() = 0.px

    val alignment get() = Alignment()

    companion object : Modifier
}

inline val Modifier.paddingWidth get() = paddingLeft + paddingRight
inline val Modifier.paddingHeight get() = paddingTop + paddingBottom
inline val Modifier.contentMinWidth get() = width.ifNan { minWidth }
inline val Modifier.contentMinHeight get() = height.ifNan { minHeight }

fun Modifier.size(width: Measure, height: Measure) = object : Modifier by this {
    override val width get() = width
    override val height get() = height
}
fun Modifier.width(width: Measure) = object : Modifier by this {
    override val width get() = width
}
fun Modifier.height(height: Measure) = object : Modifier by this {
    override val height get() = height
}
fun Modifier.minWidth(minWidth: Measure) = object : Modifier by this {
    override val minWidth get() = Measure.max(this@minWidth.minWidth, minWidth)
}
fun Modifier.minHeight(minHeight: Measure) = object : Modifier by this {
    override val minHeight get() = Measure.max(this@minHeight.minHeight, minHeight)
}
fun Modifier.minSize(minWidth: Measure, minHeight: Measure) = object : Modifier by this {
    override val minWidth get() = Measure.max(this@minSize.minWidth, minWidth)
    override val minHeight get() = Measure.max(this@minSize.minHeight, minHeight)
}
fun Modifier.weight(weight: Double) = object : Modifier by this {
    override val weight get() = weight
}
fun Modifier.align(alignment: Alignment.()-> Alignment) = object : Modifier by this {
    override val alignment = Alignment().alignment()
}
fun Modifier.padding(value: Measure) = object : Modifier by this {
    override val paddingLeft get() = this@padding.paddingLeft + value
    override val paddingTop get() = this@padding.paddingTop + value
    override val paddingRight get() = this@padding.paddingRight + value
    override val paddingBottom get() = this@padding.paddingBottom + value
}
fun Modifier.padding(h: Measure = 0.px, v: Measure = 0.px) = object : Modifier by this {
    override val paddingLeft get() = this@padding.paddingLeft + h
    override val paddingTop get() = this@padding.paddingTop + v
    override val paddingRight get() = this@padding.paddingRight + h
    override val paddingBottom get() = this@padding.paddingBottom + v
}
fun Modifier.padding(left: Measure = 0.px, top: Measure = 0.px, right: Measure = 0.px, bottom: Measure = 0.px) = object : Modifier by this {
    override val paddingLeft get() = this@padding.paddingLeft + left
    override val paddingTop get() = this@padding.paddingTop + top
    override val paddingRight get() = this@padding.paddingRight + right
    override val paddingBottom get() = this@padding.paddingBottom + bottom
}