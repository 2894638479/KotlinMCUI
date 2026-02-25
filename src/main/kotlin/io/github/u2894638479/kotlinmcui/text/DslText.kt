package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.rect.MutRect
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.lazy

open class DslText(
    override val identity: DslId,
    override val modifier: Modifier,
    val fontName: String?,
    val font: DslFont<*>,
    val ctx: DslContext,
    val chars:List<List<DslRenderableChar>>,
    val defaultLineHeight: Measure,
    val horizontalAligner: Aligner,
    val verticalAligner: Aligner
) : DslComponent {
    override val rect = MutRect()
    override var instance: DslComponent = this

    override val narratable get() = true
    override val narration get() = chars.joinToString {
        val arr = IntArray(it.size) { i -> it[i].code }
        String(arr,0,arr.size)
    }
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        val font = backend.getFont(fontName)
        lines().forEach { it.renderChars(font,it.alignedChars(font)) }
    }
    context(instance: DslComponent)
    open fun processChars(chars:List<List<DslRenderableChar>>) = chars

    private var lazyChars: List<List<DslRenderableChar>>? = null
    context(instance: DslComponent)
    val processedChars get() = ::lazyChars.lazy { processChars(chars) }

    context(instance: DslComponent)
    fun lines(): List<DslTextLine> {
        val rect = instance.rect
        return processedChars.map {
            val rect = MutRect(left = rect.left, right = rect.right)
            DslTextLine(font,rect , it, horizontalAligner,defaultLineHeight)
        }.also { verticalAligner.align(rect.top, rect.bottom,it) }
    }

    private var lazyHeight = Measure.AUTO
    private var lazyWidth = Measure.AUTO
    override val contentMinHeight get() = ::lazyHeight.lazy {
        max(processedChars.totalHeight(font, defaultLineHeight),super.contentMinHeight)
    }
    override val contentMinWidth get() = ::lazyWidth.lazy {
        max(processedChars.totalWidth(font),super.contentMinHeight)
    }
}