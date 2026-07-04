package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun TestScrollPage() = Row {
    val scrollerProp1 = local { Scroller.empty }
    Column {
        var lastClick by local<Int?> { null }
        TextAutoFold(Modifier.weight(0.0)) {
            "this is a scrollable Column".emit()
            lastClick?.let {
                enter()
                "last clicked:$lastClick".emit()
            }
        }
        ScrollableColumn(scrollerProp = scrollerProp1) {
            TextFlatten { "Scrollable Row:".emit() }
            val scrollerProp = local { Scroller.empty }
            ScrollableRow(Modifier.height(50.scaled),scrollerProp) {
                for(i in 1..20) {
                    Button(id = i) {
                        TextFlatten(Modifier.padding(3.scaled)) {
                            "item$i".emit()
                        }
                    }.clickable {  }
                }
            }
            ScrollBarHorizontal(Modifier.height(10.scaled),scrollerProp) {}
            for (i in 1..20) {
                if (i % 3 == 1) TextFlatten { "item$i".emit() }
                else Button(
                    Modifier.height(20.scaled),
                    id = i
                ) { TextFlatten { "item$i".emit() } }.clickable { lastClick = i }
            }
            TextAutoFold { "nested scrollable column:".emit() }
            Row {
                val scrollerPropNested = local { Scroller.empty }
                ScrollableColumn(Modifier.minHeight(100.scaled), scrollerProp = scrollerPropNested) {
                    for (i in 21..30) {
                        Button(
                            Modifier.height(20.scaled),
                            id = i
                        ) { TextFlatten { "item$i".emit() } }.clickable { lastClick = i }
                    }
                }
                ScrollBarVertical(Modifier.width(10.scaled), scrollerPropNested) {}
            }
        }
    }
    ScrollBarVertical(Modifier.width(10.scaled), scrollerProp1) {}


    val scrollerProp2 = local { Scroller.empty }
    Column {
        TextAutoFold(Modifier.weight(0.0)) { "this is a scrollable Column with not animatable scroll state".emit() }
        val scrollProp = local { 0.0 }
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
    ScrollBarVertical(Modifier.width(10.scaled), scrollerProp2) {}

    val scrollerProp3 = local { Scroller.empty }
    Column {
        TextAutoFold(Modifier.weight(0.0)) { "this is a lazy column".emit() }
        LazyColumn(Modifier, scrollerProp3) {
            var toggle by local { false }
            for (i in 1..20) {
                if (i % 3 == 0) TextFlatten(id = i) { "item$i".emit() }
                else Button(Modifier.height(if (toggle) 20.scaled else 40.scaled), id = i) {
                    TextFlatten { "item$i".emit() }
                }.clickable { toggle = !toggle }
            }
            val scrollerProp4 = local { Scroller.empty }
            TextAutoFold(Modifier.weight(0.0)) { "nested lazy column".emit() }
            LazyColumn(Modifier.height(150.scaled), scrollerProp4) {
                var toggle by local { false }
                for (i in 1..20) {
                    if (i % 3 == 0) TextFlatten(id = i) { "item$i".emit() }
                    else Button(Modifier.height(if (toggle) 20.scaled else 40.scaled), id = i) {
                        TextFlatten { "item$i".emit() }
                    }.clickable { toggle = !toggle }
                }
            }
            ScrollBarHorizontal(Modifier.height(10.scaled), scrollerProp4) {}
        }
        ScrollBarHorizontal(Modifier.height(10.scaled), scrollerProp3) {}
    }
    ScrollBarVertical(Modifier.width(10.scaled), scrollerProp3) {}
}