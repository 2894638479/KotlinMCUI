package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.width

internal interface MouseTipComponent

context(ctx: DslContext)
fun MouseTip(
    modifier: Modifier = Modifier,
    id: Any? = null,
    function: DslFunction
) {
    val ctx = ctx.change(dslChildren = dataStore.dslScreen.children)
    var modifier = modifier
    if(modifier.width.bits == Measure.AUTO.bits) modifier = modifier.width(Measure.AUTO_MIN)
    if(modifier.height.bits == Measure.AUTO.bits) modifier = modifier.height(Measure.AUTO_MIN)
    context(ctx) {
        Box(modifier,id,function).change { delegate ->
            object : DslComponent by delegate, MouseTipComponent {
                context(instance: DslComponent)
                override fun layoutHorizontal() {
                    val width = instance.rect.width
                    val left = when(instance.modifier.alignment.horizontal) {
                        LOW -> dataStore.mouse.x - width
                        MID -> dataStore.mouse.x - width/2
                        HIGH -> dataStore.mouse.x
                    }
                    instance.rect.left = left
                    instance.rect.right = left + width
                    delegate.layoutHorizontal()
                }
                context(instance: DslComponent)
                override fun layoutVertical() {
                    val height = instance.rect.height
                    val top = when(instance.modifier.alignment.vertical) {
                        LOW -> dataStore.mouse.y - height
                        MID -> dataStore.mouse.y - height/2
                        HIGH -> dataStore.mouse.y
                    }
                    instance.rect.top = top
                    instance.rect.bottom = top + height
                    delegate.layoutVertical()
                }
            }
        }
    }
}