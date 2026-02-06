package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.autoAnimate
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.translate
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.functions.ui.defaultBackground
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minWidth
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun TestPage() = Row {
    val pages by remember {
        mapOf<String, DslFunction>(
            "layout" to { TestLayoutPage() },
            "scroll" to { TestScrollPage() },
            "text" to { TestTextPage() },
            "slider" to { TestSliderPage() },
            "id" to { TestIdPage() },
            "image" to { TestImagePage() },
            "translation" to { TestTranslationPage() }
        ).mapKeys { translate("kotlinmcui.${it.key}") }
    }
    var page by remember(pages.entries.first())
    ScrollableColumn(Modifier.weight(1.0).minWidth(100.scaled)) {
        TextFlatten(Modifier.padding(5.scaled)) { translate("kotlinmcui.testpage").emit() }
        pages.entries.forEachWithId {
            val h by autoAnimate(if (page == it) 40.scaled else 20.scaled)
            Button(Modifier.height(h).padding(2.scaled)) { TextFlatten { it.key.emit() } }
                .clickable(page != it) { page = it }
        }
    }
    Box(Modifier.weight(2.5)) { page.value() }
}.defaultBackground()