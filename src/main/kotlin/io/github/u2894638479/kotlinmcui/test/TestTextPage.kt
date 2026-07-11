package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import kotlin.reflect.KProperty

private enum class Page{TEXT_LAYOUT,TEXT_STYLE}

context(ctx: DslContext)
fun TestTextPage() = ScrollableColumn {
    var currentPage by local { Page.TEXT_LAYOUT }
    Row(Modifier.height(20.scaled)) {
        Page.entries.forEach {
            Button(id = it) {
                TextFlatten {
                    when (it) {
                        Page.TEXT_LAYOUT -> "Text Layout"
                        Page.TEXT_STYLE -> "Text Style"
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
            var style by local { DslCharStyle() }

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
            val sizeProp = local { "" }
            val size by sizeProp.remap { it.toDoubleOrNull()?.scaled ?: 9.scaled }
            val fontNameProp = local { "" }
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
            val colorProp = local { Color.WHITE }
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
            val textProp = local { "you can edit this text" }
            EditableText(Modifier.padding(5.scaled), textProp, fontName = fontName, style = style, size = size, color = color) {}
        }
    }
}