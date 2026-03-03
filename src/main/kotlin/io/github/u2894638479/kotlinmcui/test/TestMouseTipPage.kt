package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltip
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltipBackground
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.align
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.text.DslCharStyle


context(ctx: DslContext)
fun TestMouseTipPage() = Column {
    val width by remember(100).property
    val height by remember(100).property
    var alignH by remember(Align.MID)
    var alignV by remember(Align.MID)
    MouseTip(Modifier.align { horizontal(alignH).vertical(alignV) }) {
        ColorRect(Modifier.size(width.value.scaled,height.value.scaled), Color(255, 255, 255, 80)) {}
    }
    var tip2 by remember(false)
    if(tip2) MouseTip(Modifier.size(20.scaled,20.scaled)) {
        Spacer {}.containerBackground()
    }
    Row {
        TextFlatten { "horizon:".emit() }
        Button(Modifier.height(20.scaled)) {
            TextFlatten { alignH.name.emit() }
        }.clickable { alignH = Align.entries.run { get((alignH.ordinal + 1) % size) } }
    }
    Row {
        TextFlatten { "vertical:".emit() }
        Button(Modifier.height(20.scaled)) {
            TextFlatten { alignV.name.emit() }
        }.clickable { alignV = Align.entries.run { get((alignV.ordinal + 1) % size) } }
    }
    Slider(Modifier.height(20.scaled).padding(5.scaled),Axis.Horizontal,0..200,width) {
        TextFlatten { "width:${width.value}".emit() }
    }
    Slider(Modifier.height(20.scaled).padding(5.scaled),Axis.Horizontal,0..200,height) {
        TextFlatten { "height:${height.value}".emit() }
    }
    Button(Modifier.height(20.scaled)) { TextFlatten { "tip2:$tip2".emit() } }.clickable { tip2 = !tip2 }
    var tooltipCounter by remember(2)
    Button(Modifier.height(20.scaled)) {
        TextFlatten { "this button has a tooltip".emit() }
    }.clickable { tooltipCounter++ }.tooltip {
        Column(Modifier.padding(10.scaled)) {
            TextFlatten { "Tooltip Test: $tooltipCounter".emit(Color.BLUE,18.scaled, DslCharStyle().shadowed.italic.underlined) }
            ColorRect(Modifier.height(1.scaled).padding(3.scaled),Color(180,180,180)) {}
            Button(Modifier.height(20.scaled)) {
                TextFlatten { "this is clickable now!".emit() }
            }.clickable { tooltipCounter-- }
            TextAutoFold { "123123123afdssssssssssssssssssdsaff".emit() }
        }.tooltipBackground()
    }
    Button(Modifier.height(20.scaled)) {
        TextFlatten { "this button has a tooltip".emit() }
    }.clickable {  }.tooltip {
        Column(Modifier.padding(10.scaled)) {
            TextFlatten { "ScrollableColumn:".emit(Color.BLUE,18.scaled, DslCharStyle().shadowed.italic.underlined) }
            ColorRect(Modifier.height(1.scaled).padding(3.scaled),Color(180,180,180)) {}
            ScrollableColumn(Modifier.height(100.scaled)) {
                (1..10).forEachWithId {
                    Button(Modifier.height(20.scaled)) {
                        TextFlatten { "Button $it".emit() }
                    }.clickable {  }
                }
            }
        }.tooltipBackground()
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}