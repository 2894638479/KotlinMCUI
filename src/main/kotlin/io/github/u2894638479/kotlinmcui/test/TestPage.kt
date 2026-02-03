package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.DslFunction
import io.github.u2894638479.kotlinmcui.functions.autoAnimate
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.decorator.backGroundImage
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.imageResource
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.DefaultBackground
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minWidth
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue

context(ctx: DslContext)
fun TestPage() = Row {
    class Page(val name: String, val function: DslFunction)

    val pages by remember {
        listOf(
            Page("布局") { TestLayoutPage() },
            Page("滚动") { TestScrollPage() },
            Page("文字") { TestTextPage() },
            Page("滑条") { TestSliderPage() },
            Page("id") { TestIdPage() },
            Page("图片") { TestImagePage() },
        )
    }
    var page by remember(pages.first())
    ScrollableColumn(Modifier.weight(1.0).minWidth(100.scaled)) {
        TextFlatten(Modifier.padding(5.scaled)) { "测试项目".emit() }
        pages.forEachWithId {
            val h by autoAnimate(if (page == it) 40.scaled else 20.scaled)
            Button(Modifier.height(h).padding(2.scaled)) { TextFlatten { it.name.emit() } }
                .clickable(page != it) { page = it }
        }
    }
    Box(Modifier.weight(2.5)) { page.function() }
    DefaultBackground()
}