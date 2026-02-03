package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.LazyColumn
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollBar
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollBarHorizontal
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minHeight
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun TestScrollPage() = Row {
    val scrollerProp1 by Scroller.empty.remember.property
    Column {
        var lastClick by remember<Int?>(null)
        TextAutoFold(Modifier.weight(0.0)) {
            "this is a scrollable Column".emit()
            lastClick?.let {
                enter()
                "last clicked:$lastClick".emit()
            }
        }
        ScrollableColumn(scrollerProp = scrollerProp1) {
            for (i in 1..20) {
                if (i % 3 == 1) TextFlatten { "item$i".emit() }
                else Button(
                    Modifier.height(20.scaled),
                    id = i
                ) { TextFlatten { "item$i".emit() } }.clickable { lastClick = i }
            }
            TextAutoFold { "nested scrollable column:".emit() }
            Row {
                val scrollerPropNested by Scroller.empty.remember.property
                ScrollableColumn(Modifier.minHeight(100.scaled), scrollerProp = scrollerPropNested) {
                    for (i in 21..30) {
                        Button(
                            Modifier.height(20.scaled),
                            id = i
                        ) { TextFlatten { "item$i".emit() } }.clickable { lastClick = i }
                    }
                }
                ScrollBar(Modifier.width(10.scaled), scrollerPropNested) {}
            }
        }
    }
    ScrollBar(Modifier.width(10.scaled), scrollerProp1) {}


    val scrollerProp2 by Scroller.empty.remember.property
    Column {
        TextAutoFold(Modifier.weight(0.0)) { "this is a scrollable Column with not animatable scroll state".emit() }
        val scrollProp by remember(0.0).property
        ScrollableColumn(scrollerProp = scrollerProp2, scrollProp = scrollProp) {
            for (i in 1..20) {
                if (i % 3 == 0) TextFlatten(id = i) { "item$i".emit() }
                else Button(
                    Modifier.height(20.scaled),
                    id = i
                ) { TextFlatten { "item$i".emit() } }.clickable { }
            }
        }
    }
    ScrollBar(Modifier.width(10.scaled), scrollerProp2) {}

    val scrollerProp3 by Scroller.empty.remember.property
    Column {
        TextAutoFold(Modifier.weight(0.0)) { "this is a lazy column".emit() }
        LazyColumn(Modifier, scrollerProp3) {
            var toggle by false.remember
            for (i in 1..20) {
                if (i % 3 == 0) TextFlatten(id = i) { "item$i".emit() }
                else Button(Modifier.height(if (toggle) 20.scaled else 40.scaled), id = i) {
                    TextFlatten { "item$i".emit() }
                }.clickable { toggle = !toggle }
            }
        }
        ScrollBarHorizontal(Modifier.height(10.scaled), scrollerProp3) {}
    }
    ScrollBar(Modifier.width(10.scaled), scrollerProp3) {}
}