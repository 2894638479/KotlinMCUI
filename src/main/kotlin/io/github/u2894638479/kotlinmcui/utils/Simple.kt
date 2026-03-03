package io.github.u2894638479.kotlinmcui.utils

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.DslExecuteContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltip
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltipBackground
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.ColorRect
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Spacer
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minSize
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.kotlinmcui.text.DslCharStyle

object Simple {
    context(ctx: DslContext)
    fun Splitter(color: Color = Color(180,180,180),size:Measure = 1.scaled, padding: Measure = 3.scaled, id: Any) =
        ColorRect(Modifier.minSize(size,size).weight(0.0).padding(padding),color,id)

    context(ctx: DslContext)
    fun Button(text: String, active: Boolean = true, id: Any? = null, onClick:context(DslExecuteContext)() -> Unit) = Button(Modifier.height(20.scaled).padding(2.scaled),id = id ?: onClick::class) {
        TextFlatten(Modifier.padding(h = 10.scaled)) { text.emit() }
    }.clickable(active,onClick)

    context(ctx: DslContext)
    fun Spacer(size: Measure = 0.px,id: Any) = Spacer(Modifier.size(size,size),id)

    context(ctx: DslContext)
    fun Text(id:Any? = null, text:()-> String) = TextFlatten(id = id ?: text::class) { text().emit() }

    context(ctx: DslContext)
    fun DslChild.simpleTooltip(content: DslFunction) = tooltip {
        Box(Modifier.padding(10.scaled),id = null) { content() }.tooltipBackground()
    }

    context(ctx: DslContext)
    fun DslChild.simpleTooltip(text: String) = simpleTooltip { Text { text } }

    context(ctx: DslContext)
    fun DslChild.simpleTooltip(title: String,content: DslFunction) = tooltip {
        Column(Modifier.padding(10.scaled)) {
            TextFlatten(Modifier.padding(h = 10.scaled)) { title.emit(Color.BLUE,18.scaled, DslCharStyle().italic.shadowed) }
            Splitter {}
            Box(modifier = Modifier) { content() }
        }.tooltipBackground()
    }

    context(ctx: DslContext)
    fun DslChild.simpleTooltip(title: String,text: String) = simpleTooltip(title) { Text { text } }
}