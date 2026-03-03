package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.defaultOnClose
import io.github.u2894638479.kotlinmcui.context.onClose
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.remember
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
    var counter by 0.remember

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
            var closable by true.remember
            var info by remember<DslFunction> {{}}
            Column {
                info()
                Simple.Button("closable:$closable") { closable = !closable }
                Simple.Button("close this screen(should be equal to Esc)") {
                    dataStore.onClose()
                }
                TextFlatten { "variable from prev screen:".emit() }
                CounterModify {}
                Spacer(Modifier.weight(Double.MAX_VALUE)) {}
            }.defaultBackground()
            var triedTimes by remember(0)
            onClose {
                if(closable) defaultOnClose()
                else info = {
                    TextAutoFold {
                        "close is stopped in onClose(), tried times $triedTimes".emit(Color.RED)
                    }
                }
            }
        }
    }
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}