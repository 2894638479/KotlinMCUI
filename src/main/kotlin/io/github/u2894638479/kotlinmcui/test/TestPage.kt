package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.*
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip

context(ctx: DslContext)
fun TestPage(): DslChild = Row {
    val pages by static {
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
            "color" to { TestColorPage() },
            "metadata" to { TestMetadataPage() },
            "rotation" to { TestRotationPage() },
            "animation" to { TestAnimationPage() }
        ).mapKeys { translate("kotlinmcui.${it.key}") }
    }
    var page by local { pages.entries.first() }
    ScrollableColumn(Modifier weight 1.0 minWidth 100.scaled) {
        TextFlatten(Modifier padding 5.scaled) { translate("kotlinmcui.testpage").emit() }
        pages.entries.forEachWithId {
            val h by local.autoAnimate { if (page == it) 40.0 else 20.0 }
            Button(Modifier height h.scaled padding 2.scaled) { TextFlatten { it.key.emit() } }
                .clickable(page != it) { page = it }.simpleTooltip(it.key)
        }
    }
    Box(Modifier weight 2.5) { page.value() }
}.defaultBackground()