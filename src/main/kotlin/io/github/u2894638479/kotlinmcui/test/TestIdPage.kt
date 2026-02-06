package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.scope.DslChild

context(ctx: DslContext)
fun TestIdPage() = Row {
    var id by remember(DslId(null))

    ScrollableColumn {
        TextAutoFold { "last clicked id: $id".emit() }
        TextAutoFold { "focused id: ${ctx.dataStore.focused}".emit(color = Color(50, 250, 20)) }
        TextAutoFold { "hovered id: ${ctx.dataStore.hovered}".emit(color = Color(200, 30, 30)) }
    }

    Column(Modifier.weight(2.0)) {
        context(ctx: DslContext)
        fun DslChild.clickId() = clickable { id = currentComponent().identity }
        Button { TextFlatten { "Column$1".emit() } }.clickId()
        Button { TextFlatten { "Column$2".emit() } }.clickId()
        Button {
            Column(Modifier.padding(5.scaled)) {
                Button { TextFlatten { $$"Column$Button$Column$1".emit() } }.clickId()
                Button { TextFlatten { $$"Column$Button$Column$1".emit() } }.clickId()
            }
        }.clickId()
        Row {
            Button { TextFlatten { "Column\$Row$1".emit() } }.clickId()
            Button { TextFlatten { "Column\$Row$2".emit() } }.clickId()
        }
        Column {
            Button { TextFlatten { "Column\$Column$1".emit() } }.clickId()
            Button { TextFlatten { "Column\$Column$2".emit() } }.clickId()
        }
        Box {
            Button(Modifier.height(20.scaled).align { bottom() }) {
                TextFlatten { "Column\$Box$1".emit() }
            }.clickId()
            Button(Modifier.height(20.scaled).align { top() }) {
                TextFlatten { "Column\$Box$2".emit() }
            }.clickId()
        }
        SliderHorizontal {}.Box {
            Button(Modifier.height(20.scaled).padding(5.scaled).align { bottom() }) {
                TextFlatten { "Column\$Slider.Box$1".emit() }
            }.clickId()
            Button(Modifier.height(20.scaled).padding(5.scaled).align { top() }) {
                TextFlatten { "Column\$Slider.Box$2".emit() }
            }.clickId()
        }
    }
}