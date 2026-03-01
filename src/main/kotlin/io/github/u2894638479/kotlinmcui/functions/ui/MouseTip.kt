package io.github.u2894638479.kotlinmcui.functions.ui

import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.math.align.Align.*
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
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
    val ctx = ctx.change(children = dataStore.dslScreen.children)
    var modifier = modifier
    if(modifier.width.bits == Measure.AUTO.bits) modifier = modifier.width(Measure.AUTO_MIN)
    if(modifier.height.bits == Measure.AUTO.bits) modifier = modifier.height(Measure.AUTO_MIN)
    context(ctx) {
        Box(modifier,id,function).change { delegate ->
            object : DslComponent by delegate, MouseTipComponent {
                override fun layoutHorizontal() {
                    val rect = instance.rect
                    val width = rect.width
                    val left = when(instance.modifier.alignment.horizontal) {
                        LOW -> dataStore.mouse.x - width
                        MID -> dataStore.mouse.x - width/2
                        HIGH -> dataStore.mouse.x
                    }
                    rect.left = left
                    rect.right = left + width
                    delegate.layoutHorizontal()
                }
                override fun layoutVertical() {
                    val rect = instance.rect
                    val height = rect.height
                    val top = when(instance.modifier.alignment.vertical) {
                        LOW -> dataStore.mouse.y - height
                        MID -> dataStore.mouse.y - height/2
                        HIGH -> dataStore.mouse.y
                    }
                    rect.top = top
                    rect.bottom = top + height
                    delegate.layoutVertical()
                }
            }
        }
    }
}