package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.widget.Editors
import io.github.u2894638479.kotlinmcui.widget.Presets
import io.github.u2894638479.kotlinmcui.widget.Presets.styledTooltip


context(ctx: DslContext)
fun TestMouseTipPage() = Column {
    val width = local { 100 }
    val height = local { 100 }
    val alignH = local { Align.MID }
    val alignV = local { Align.MID }
    MouseTip(Modifier.align { horizontal(alignH.value).vertical(alignV.value) }) {
        ColorRect(Modifier.size(width.value.scaled,height.value.scaled), Color(255, 255, 255, 80)) {}
    }
    val tip2 = local { false }
    if(tip2.value) MouseTip(Modifier.size(20.scaled,20.scaled)) {
        Spacer {}.containerBackground()
    }
    Row {
        Editors.EnumButton(alignH,"horizontal") {}
        Editors.EnumButton(alignV,"vertical") {}
    }
    Row {
        Editors.Slider(width,0..200,"width") {}
        Editors.Slider(height,0..200,"height") {}
    }
    Editors.BoolButton(tip2,"tip2") {}
    var tooltipCounter by local { 2 }

    Presets.Button("this button has a tooltip") { tooltipCounter++ }.styledTooltip("Tooltip Test: $tooltipCounter") {
        Column {
            Presets.Button("this is clickable now!"){ tooltipCounter-- }
            TextAutoFold { "123123123afdssssssssssssssssssdsaff".emit() }
        }
    }
    Presets.Button("this button has a tooltip") {}.styledTooltip("ScrollableColumn: ") {
        Column {
            ScrollableColumn(Modifier.height(100.scaled)) {
                (1..10).forEachWithId {
                    Presets.Button("Button $it") {}
                }
            }
        }
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}