package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.translate
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.onSet
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import kotlin.math.roundToInt

context(ctx: DslContext)
fun TestTranslationPage() = ScrollableColumn {
    Spacer(Modifier.height(10.scaled)) {}
    val stringProp by "test".remember.property
    var string by stringProp
    val doubleProp by 0.5.remember.property
    var double by doubleProp
    TextAutoFold {
        translate("kotlinmcui.test_translate",string,(double*10).roundToInt(),double).emit()
    }
    Spacer(Modifier.height(10.scaled)) {}
    EditableText(Modifier.height(25.scaled),stringProp) {}.editBoxBackground()
    Spacer(Modifier.height(10.scaled)) {}
    SliderHorizontal(Modifier.height(25.scaled),doubleProp) { TextFlatten { double.toString().emit() } }
    Spacer(Modifier.height(30.scaled)) {}
    val customTranslationId by "kotlinmcui.testpage".remember.property
    val keys by remember(mutableListOf(""))
    TextFlatten { "result:${translate(customTranslationId.value,*keys.toTypedArray())}".emit() }
    Row {
        TextFlatten { "custom id:".emit() }
        EditableText(Modifier,customTranslationId) {}
    }.editBoxBackground()
    TextFlatten(Modifier.padding(5.scaled)) { "%s values:".emit() }
    Row(Modifier.height(20.scaled)) {
        Button { TextFlatten { "add".emit() } }.clickable { keys += "" }
        Button { TextFlatten { "remove".emit() } }.clickable { keys.removeLastOrNull() }
    }
    keys.indices.forEachWithId { index ->
        val value by remember("").property
        value.value = keys[index]
        EditableText(Modifier.padding(5.scaled),value.onSet { keys[index] = it }) {}.editBoxBackground()
    }
}