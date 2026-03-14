package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.align
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip


context(ctx: DslContext)
fun TestMouseTipPage() = Column {
    val width by remember(100).property
    val height by remember(100).property
    val alignH by remember(Align.MID).property
    val alignV by remember(Align.MID).property
    MouseTip(Modifier.align { horizontal(alignH.value).vertical(alignV.value) }) {
        ColorRect(Modifier.size(width.value.scaled,height.value.scaled), Color(255, 255, 255, 80)) {}
    }
    val tip2 by remember(false).property
    if(tip2.value) MouseTip(Modifier.size(20.scaled,20.scaled)) {
        Spacer {}.containerBackground()
    }
    Row {
        Config.EnumButton(alignH,"horizontal") {}
        Config.EnumButton(alignV,"vertical") {}
    }
    Row {
        Config.Slider(width,0..200,"width") {}
        Config.Slider(height,0..200,"height") {}
    }
    Config.BoolButton(tip2,"tip2") {}
    var tooltipCounter by remember(2)

    Simple.Button("this button has a tooltip") { tooltipCounter++ }.simpleTooltip("Tooltip Test: $tooltipCounter") {
        Column {
            Simple.Button("this is clickable now!"){ tooltipCounter-- }
            TextAutoFold { "123123123afdssssssssssssssssssdsaff".emit() }
        }
    }
    Simple.Button("this button has a tooltip") {}.simpleTooltip("ScrollableColumn: ") {
        Column {
            ScrollableColumn(Modifier.height(100.scaled)) {
                (1..10).forEachWithId {
                    Simple.Button("Button $it") {}
                }
            }
        }
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}