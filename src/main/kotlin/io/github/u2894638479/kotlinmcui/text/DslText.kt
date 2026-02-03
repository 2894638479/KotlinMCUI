package io.github.u2894638479.kotlinmcui.text

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.Rect
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.ContextLazy

open class DslText(
    override val identity: DslId,
    override val modifier: Modifier,
    val fontName: String?,
    val font: DslFont<*>,
    val chars:List<List<DslRenderableChar>>,
    val defaultLineHeight: Measure,
    val horizontalAligner: Aligner,
    val verticalAligner: Aligner,
    override val rect: Rect = Rect()
) : DslComponent {
    context(backend: DslBackendRenderer<RP>, renderPara: RP, instance: DslComponent)
    override fun <RP> render(mouse: Position) {
        val font = backend.getFont(fontName)
        lines().forEach { it.renderChars(font,it.alignedChars(font)) }
    }
    context(instance: DslComponent)
    open fun processChars(chars:List<List<DslRenderableChar>>) = chars
    private val lazyChars = ContextLazy<DslComponent, List<List<DslRenderableChar>>> { processChars(chars) }

    context(instance: DslComponent)
    val processedChars get() = lazyChars.value

    context(instance: DslComponent)
    fun lines(): List<DslTextLine> {
        val rect = instance.rect
        return processedChars.map {
            val rect = Rect(left = rect.left, right = rect.right)
            DslTextLine(font,rect , it, horizontalAligner,defaultLineHeight)
        }.also { verticalAligner.align(rect.top, rect.bottom,it) }
    }

    private val lazyHeight = ContextLazy<DslComponent, Measure> { processedChars.totalHeight(font, defaultLineHeight) }
    private val lazyWidth = ContextLazy<DslComponent, Measure> { processedChars.totalWidth(font) }

    context(instance: DslComponent)
    override val contentMinHeight get() = Measure.max(lazyHeight.value, super.contentMinHeight)

    context(instance: DslComponent)
    override val contentMinWidth get() = Measure.max(lazyWidth.value, super.contentMinHeight)
}