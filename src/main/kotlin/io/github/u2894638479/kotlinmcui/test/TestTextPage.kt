package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.Button
import io.github.u2894638479.kotlinmcui.functions.ui.ColorRect
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.EditableText
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.Spacer
import io.github.u2894638479.kotlinmcui.functions.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.functions.ui.TextFoldable
import io.github.u2894638479.kotlinmcui.functions.ui.editBoxBackground
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.minHeight
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import kotlin.reflect.KProperty

private enum class Page{TEXT_LAYOUT,TEXT_STYLE}

context(ctx: DslContext)
fun TestTextPage() = ScrollableColumn {
    var currentPage by Page.TEXT_LAYOUT.remember
    Row(Modifier.height(20.scaled)) {
        Page.entries.forEach {
            Button(id = it) {
                TextFlatten {
                    when (it) {
                        Page.TEXT_LAYOUT -> "文字布局"
                        Page.TEXT_STYLE -> "文字样式"
                    }.emit()
                }
            }.clickable(currentPage != it) { currentPage = it }
        }
    }
    when (currentPage) {
        Page.TEXT_LAYOUT -> Column {
            ColorRect(Modifier.height(1.px), Color.WHITE) {}
            TextFoldable {
                repeat(3) {
                    "this text is foldable, but only when \\n or enter()".emit()
                    enter()
                }
            }
            ColorRect(Modifier.height(1.px), Color.WHITE) {}
            TextAutoFold {
                repeat(3) {
                    "this text is foldable, and will automatically split into new lines".emit()
                    enter()
                }
            }
            ColorRect(Modifier.height(1.px), Color.WHITE) {}
            TextFlatten {
                "this \n text \n will never\n fold. ".emit()
                enter()
            }
            ColorRect(Modifier.height(1.px), Color.WHITE) {}
            Spacer(Modifier.weight(Double.MAX_VALUE)) {}
        }

        Page.TEXT_STYLE -> Column {
            var style by remember(DslCharStyle())

            context(ctx: DslContext)
            fun styleChangeButton(
                prop: KProperty<Boolean>,
                value: Boolean,
                change: DslCharStyle.(Boolean) -> DslCharStyle
            ) = Button(Modifier.minHeight(20.scaled),id = prop) {
                TextFlatten {
                    prop.name.emit()
                    ": ".emit()
                    value.toString().emit(color = if (value) Color.GREEN else Color.RED)
                }
            }.clickable { style = style.change(!value) }
            Row {
                styleChangeButton(DslCharStyle::isItalic, style.isItalic, DslCharStyle::changeItalic)
                styleChangeButton(DslCharStyle::isBold, style.isBold, DslCharStyle::changeBold)
                styleChangeButton(DslCharStyle::isUnderlined, style.isUnderlined, DslCharStyle::changeUnderlined)
            }
            Row {
                styleChangeButton(DslCharStyle::isStrikeThrough, style.isStrikeThrough, DslCharStyle::changeStrikeThrough)
                styleChangeButton(DslCharStyle::isObfuscated, style.isObfuscated, DslCharStyle::changeObfuscated)
                styleChangeButton(DslCharStyle::isShadowed, style.isShadowed, DslCharStyle::changeShadowed)
            }
            val sizeProp by "".remember.property
            val size by sizeProp.remap { it.toDoubleOrNull()?.scaled ?: 9.scaled }
            val fontNameProp by "".remember.property
            val fontName by fontNameProp
            Row {
                Column {
                    TextAutoFold { "Font name: $fontName".emit() }
                    EditableText(Modifier.padding(5.scaled), fontNameProp) {}.editBoxBackground()
                }
                Column {
                    TextAutoFold { "Font size: $size".emit() }
                    EditableText(Modifier.padding(5.scaled), sizeProp) {}.editBoxBackground()
                }
            }
            val colorProp by Color.WHITE.remember.property
            var color by colorProp
            Row {
                Spacer {}
                TextFlatten { "color argb:#${color.hexStringARGB}".emit() }
                ColorRect(Modifier.size(50.scaled, 25.scaled).padding(5.scaled), color = color) {}
                Spacer {}
            }
            Row(Modifier.height(20.scaled)) {
                TextFlatten { "a:".emit() }
                EditableText(Modifier, colorProp.remap({ it.a.toString() }, { it.toUByteOrNull()?.let { color.change(a = it) } ?: color })) {}
                TextFlatten { "r:".emit(color = Color.RED) }
                EditableText(Modifier, colorProp.remap({ it.r.toString() }, { it.toUByteOrNull()?.let { color.change(r = it) } ?: color }), color = Color.RED) {}
                TextFlatten { "g:".emit(color = Color.GREEN) }
                EditableText(Modifier, colorProp.remap({ it.g.toString() }, { it.toUByteOrNull()?.let { color.change(g = it) } ?: color }), color = Color.GREEN) {}
                TextFlatten { "b:".emit(color = Color.BLUE) }
                EditableText(Modifier, colorProp.remap({ it.b.toString() }, { it.toUByteOrNull()?.let { color.change(b = it) } ?: color }), color = Color.BLUE) {}
            }.editBoxBackground()
            val textProp by remember("you can edit this text").property
            EditableText(Modifier.padding(5.scaled), textProp, fontName = fontName, style = style, size = size, color = color) {}
        }
    }
}