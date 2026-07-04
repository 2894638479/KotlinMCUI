package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.closeScreen
import io.github.u2894638479.kotlinmcui.context.defaultOnClose
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.showScreen
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
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
            var closable by local { true }
            var info by local<DslFunction> {{}}
            Column {
                info()
                Simple.Button("closable:$closable") { closable = !closable }
                Simple.Button("close this screen(should be equal to Esc)") {
                    closeScreen()
                }
                TextFlatten { "variable from prev screen:".emit() }
                CounterModify {}
                Spacer(Modifier.weight(Double.MAX_VALUE)) {}
            }.defaultBackground()
            var triedTimes by local { 0 }
        }
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}