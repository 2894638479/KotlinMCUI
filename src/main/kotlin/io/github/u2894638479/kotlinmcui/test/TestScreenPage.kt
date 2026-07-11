package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.closeScreen
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.showScreen
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.utils.Simple

context(ctx: DslContext)
fun TestScreenPage() = Column {
    var counter by local { 0 }

    context(ctx:DslContext)
    fun CounterModify(id:Any) = Row(id = id) {
        Simple.Button("counter++") { counter++ }
        Simple.Text { "counter: $counter" }
        Simple.Button("conter--") { counter-- }
    }

    CounterModify {}

    Button(Modifier.height(20.scaled).padding(5.scaled)) {
        TextFlatten { "Go To Screen1".emit() }
    }.clickable {
        showScreen("Screen1") {
            Column {
                Simple.Button("back") {
                    closeScreen()
                }
                TextFlatten { "variable from prev screen:".emit() }
                CounterModify {}
                Spacer(Modifier.weight(Double.MAX_VALUE)) {}
            }.defaultBackground()
        }
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}