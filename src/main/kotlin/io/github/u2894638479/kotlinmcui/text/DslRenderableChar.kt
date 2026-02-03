package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px

class DslRenderableChar(
    val code: Int,
    val style: DslCharStyle,
    val color: Color,
    val size: Measure
)

fun Collection<DslRenderableChar>.maxHeight(font: DslFont<*>) = maxOfOrNull { font.charHeight(it) }
fun Collection<DslRenderableChar>.sumAdvance(font: DslFont<*>) = sumOf { font.charAdvance(it).pixelsOrElse { 0.0 } }.px

fun Collection<Collection<DslRenderableChar>>.totalHeight(font: DslFont<*>,defaultLineHeight: Measure) = sumOf {
    (it.maxOfOrNull { font.charHeight(it) } ?: defaultLineHeight).pixelsOrElse { 0.0 } }.px
fun Collection<Collection<DslRenderableChar>>.totalWidth(font: DslFont<*>) = maxOfOrNull { it.sumAdvance(font) } ?: 0.px