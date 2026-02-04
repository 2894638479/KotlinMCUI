package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.translate
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.EditableText
import io.github.u2894638479.kotlinmcui.functions.ui.SliderHorizontal
import io.github.u2894638479.kotlinmcui.functions.ui.Spacer
import io.github.u2894638479.kotlinmcui.functions.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.functions.ui.editBoxBackground
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.math.roundToInt

context(ctx: DslContext)
fun TestTranslationPage() = Column {
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
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}