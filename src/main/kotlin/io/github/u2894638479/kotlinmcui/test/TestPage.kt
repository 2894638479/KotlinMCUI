package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.*
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltip
import io.github.u2894638479.kotlinmcui.functions.decorator.tooltipBackground
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.modifier.*
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
            "translation" to { TestTranslationPage() },
            "container" to { TestContainerPage() },
            "mousetip" to { TestMouseTipPage() },
            "screen" to { TestScreenPage() },
            "color" to { TestColorPage() }
        ).mapKeys { translate("kotlinmcui.${it.key}") }
    }
    var page by remember(pages.entries.first())
    ScrollableColumn(Modifier.weight(1.0).minWidth(100.scaled)) {
        TextFlatten(Modifier.padding(5.scaled)) { translate("kotlinmcui.testpage").emit() }
        pages.entries.forEachWithId {
            val h by autoAnimate(if (page == it) 40.0 else 20.0)
            Button(Modifier.height(h.scaled).padding(2.scaled)) { TextFlatten { it.key.emit() } }
                .clickable(page != it) { page = it }.tooltip {
                    TextFlatten(Modifier.padding(10.scaled)) { it.key.emit() }.tooltipBackground()
                }
        }
    }
    Box(Modifier.weight(2.5)) { page.value() }
}.defaultBackground()