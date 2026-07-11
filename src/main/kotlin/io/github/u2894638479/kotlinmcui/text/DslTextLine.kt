package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Alignable
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.MutRect

class DslTextLine(
    val font: DslFont<*>,
    val rect: MutRect,
    val chars: List<DslRenderableChar>,
    val horizontalAlign: Aligner,
    val defaultLineHeight: Measure
): Alignable {
    fun <RP> alignedChars(font: DslFont<RP>) = chars.map {
        object : AlignableChar(it) {
            override val minSize get() = font.charAdvance(char)
            override val size get() = Measure.AUTO
            override val weight get() = 1.0
            override var low = 0.px
            override var high = 0.px
            override val align get() = Align.MID
        }
    }.also { horizontalAlign.align(rect.left,rect.right,it) }

    context(renderParam:RP)
    fun <RP> renderChars(font: DslFont<RP>,chars:List<AlignableChar>) {
        if(this.font !== font) dslLogger.warn("using different font for layout and render")
        chars.forEach { font.renderChar(it.char,(it.low+it.high-it.minSize)/2,rect.top,it.low,it.high) }
    }

    override val minSize by lazy {
        chars.maxHeight(font) ?: defaultLineHeight
    }
    override val size get() = Measure.AUTO
    override val weight get() = 1.0
    override var low by rect::top
    override var high by rect::bottom
    override val align get() = Align.MID
}