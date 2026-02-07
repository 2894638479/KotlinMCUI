package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.decorator.slotBackground
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.value

context(ctx: DslContext)
fun TestContainerPage() = ScrollableColumn {
    Row {
        val size by 10.remember.property
        SliderHorizontal(Modifier.height(20.scaled),0..100,size) {
            TextFlatten { "slot size:${size.value}".emit() }
        }
        Spacer(Modifier.size(size.value.scaled,size.value.scaled)) {}.slotBackground()
    }
    Row {
        val width by 80.remember.property
        val height by 50.remember.property
        Column {
            SliderHorizontal(Modifier.height(20.scaled),0..200,width) {
                TextFlatten { "container width:${width.value}".emit() }
            }
            SliderHorizontal(Modifier.height(20.scaled),0..100,height) {
                TextFlatten { "container height:${height.value}".emit() }
            }
        }
        Spacer(Modifier.size(width.value.scaled,height.value.scaled)) {}.containerBackground()
    }
    Column(Modifier.width(Measure.AUTO_MIN)) {
        val text by "text".remember.property
        EditableText(Modifier.padding(4.scaled),text) {}.slotBackground(2.scaled)
        (0..<4).forEachWithId {
            Row {
                (0..<9).forEachWithId {
                    Spacer(Modifier.size(18.scaled,18.scaled)) {}.clickable {

                    }.slotBackground().hoverMask()
                }
            }
            if(it == 2) Spacer(Modifier.height(4.scaled)) {}
        }
    }.containerBackground(7.scaled)
}