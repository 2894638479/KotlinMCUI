package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslTextBuilderContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.collect
import io.github.u2894638479.kotlinmcui.functions.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.text.DslRenderableChar
import io.github.u2894638479.kotlinmcui.text.DslText

context(ctx: DslContext)
fun TextFlatten(
    modifier: Modifier = Modifier,
    defaultLineHeight: Measure = 9.scaled,
    horizontalAligner: Aligner = Aligner.close(Align.MID),
    verticalAligner: Aligner = Aligner.close(Align.MID),
    fontName:String? = null,
    id:Any? = null,
    build: DslTextBuilderContext.()->Unit
) = collect(
    DslText(
        newChildId(id ?: build::class), modifier, fontName,
        ctx.dataStore.backend.getFont(fontName),
        listOf(DslTextBuilderContext(ctx).apply { build() }.toChars().flatten()),
        defaultLineHeight, horizontalAligner, verticalAligner
    )
)


context(ctx: DslContext)
fun TextFoldable(
    modifier: Modifier = Modifier,
    defaultLineHeight: Measure = 9.scaled,
    horizontalAligner: Aligner = Aligner.close(Align.MID),
    verticalAligner: Aligner = Aligner.close(Align.MID),
    fontName:String? = null,
    id:Any? = null,
    build: DslTextBuilderContext.()->Unit
) = collect(
    DslText(
        newChildId(id ?: build::class), modifier, fontName,
        ctx.dataStore.backend.getFont(fontName),
        DslTextBuilderContext(ctx).apply { build() }.toChars(),
        defaultLineHeight, horizontalAligner, verticalAligner
    )
)


context(ctx: DslContext)
fun TextAutoFold(
    modifier: Modifier = Modifier,
    defaultLineHeight: Measure = 9.scaled,
    horizontalAligner: Aligner = Aligner.close(Align.MID),
    verticalAligner: Aligner = Aligner.close(Align.MID),
    fontName:String? = null,
    id:Any? = null,
    build: DslTextBuilderContext.()->Unit
) = collect(object : DslText(
    newChildId(id ?: build::class), modifier, fontName,
    ctx.dataStore.backend.getFont(fontName),
    DslTextBuilderContext(ctx).apply { build() }.toChars(),
    defaultLineHeight, horizontalAligner, verticalAligner
) {
    context(instance: DslComponent)
    override val contentMinWidth get() = 0.px

    context(instance: DslComponent)
    override fun processChars(chars: List<List<DslRenderableChar>>): List<List<DslRenderableChar>> {
        val font = ctx.dataStore.backend.getFont(fontName)
        val result = mutableListOf<MutableList<DslRenderableChar>>()
        val maxWidth = instance.rect.width
        chars.forEach {
            result += mutableListOf<DslRenderableChar>()
            var advance = 0.px
            it.forEach {
                advance += font.charAdvance(it)
                if (advance > maxWidth) {
                    result += mutableListOf<DslRenderableChar>()
                    advance = font.charAdvance(it)
                }
                result.last() += it
            }
        }
        return result
    }
})