package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.backend.fillRect
import io.github.u2894638479.kotlinmcui.backend.fillRectGradient
import io.github.u2894638479.kotlinmcui.backend.fillPolygon
import io.github.u2894638479.kotlinmcui.backend.triangles
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.decorator.containerBackground
import io.github.u2894638479.kotlinmcui.dsl.decorator.move
import io.github.u2894638479.kotlinmcui.dsl.decorator.outline
import io.github.u2894638479.kotlinmcui.dsl.decorator.perspective
import io.github.u2894638479.kotlinmcui.dsl.decorator.renderScissor
import io.github.u2894638479.kotlinmcui.dsl.decorator.rotate
import io.github.u2894638479.kotlinmcui.dsl.decorator.scale
import io.github.u2894638479.kotlinmcui.dsl.decorator.scissor
import io.github.u2894638479.kotlinmcui.dsl.decorator.shear
import io.github.u2894638479.kotlinmcui.dsl.decorator.slotBackground
import io.github.u2894638479.kotlinmcui.dsl.decorator.tooltipBackground
import io.github.u2894638479.kotlinmcui.dsl.imageResource
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.ui.Box
import io.github.u2894638479.kotlinmcui.dsl.ui.Button
import io.github.u2894638479.kotlinmcui.dsl.ui.ColorRect
import io.github.u2894638479.kotlinmcui.dsl.ui.Column
import io.github.u2894638479.kotlinmcui.dsl.ui.EditableText
import io.github.u2894638479.kotlinmcui.dsl.ui.Image
import io.github.u2894638479.kotlinmcui.dsl.ui.Item
import io.github.u2894638479.kotlinmcui.dsl.ui.Row
import io.github.u2894638479.kotlinmcui.dsl.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.dsl.ui.TextAutoFold
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFoldable
import io.github.u2894638479.kotlinmcui.dsl.withColor
import io.github.u2894638479.kotlinmcui.dsl.ui.defaultBackground
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.math.rect.Rect
import io.github.u2894638479.kotlinmcui.math.rect.height
import io.github.u2894638479.kotlinmcui.math.rect.width
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.text.DslCharStyle

/**
 * Every rendering capability a backend has, on one page.
 *
 * This is a contract check rather than a demo: each block names the member of [DslBackendRenderer]
 * it goes through, so a screenshot of the page says which of them a given backend really draws.
 * Some of these fail only in a packaged client while looking fine in development (the tooltip
 * background and the triangle list both behaved that way), which is why they sit here next to the
 * ones that always work.
 */
context(ctx: DslContext)
fun TestBackendRenderPage() = ScrollableColumn {
    Section("quad：纯色 / 渐变（fillRect、fillRectGradient）") {
        Row {
            ColorRect(Modifier.size(50.scaled,24.scaled),Color(200,60,60),"solid")
            Spacer(Modifier.width(6.scaled)) {}
            GradientBox()
            Spacer(Modifier.width(6.scaled)) {}
            ColorRect(Modifier.size(50.scaled,24.scaled),Color(60,60,200,120),"alpha")
        }
    }
    Section("quad + 贴图：clip / repeatUV / nineSlice（renderQuad(image)）") {
        Row {
            Image(Modifier.size(24.scaled,24.scaled),imageResource("minecraft:textures/block/dirt.png",16.px,16.px),id = "img-clip")
            Spacer(Modifier.width(6.scaled)) {}
            Image(Modifier.size(64.scaled,24.scaled),imageResource("minecraft:textures/block/dirt.png",16.px,16.px),
                ImageStrategy.repeatUV(Rect(0.px,0.px,16.px,16.px),1.0),id = "img-repeat")
            Spacer(Modifier.width(6.scaled)) {}
            Image(Modifier.size(64.scaled,24.scaled),imageResource("minecraft:textures/block/dirt.png",16.px,16.px),
                // Nine-slice over a texture every version has: the outer rect is the whole 16x16 tile
                // and the inner one is it shrunk by 4px, so the four corners keep their size.
                ImageStrategy.nineSlice(Rect(0.px,0.px,16.px,16.px),Rect(4.px,4.px,12.px,12.px),ctx.scale),
                id = "img-nineslice")
        }
    }
    Section("原版背景：renderContainer / renderSlot / renderTooltip / 背景色") {
        Row {
            Spacer(Modifier.size(50.scaled,24.scaled),"plain").background(Color(40,40,40))
            Spacer(Modifier.width(6.scaled)) {}
            Spacer(Modifier.size(50.scaled,24.scaled),"container").containerBackground()
            Spacer(Modifier.width(6.scaled)) {}
            Spacer(Modifier.size(50.scaled,24.scaled),"slot").slotBackground()
            Spacer(Modifier.width(6.scaled)) {}
            // renderTooltip always draws something, so this box is where a backend that lost the
            // tooltip background shows up without anyone having to hover anything.
            Spacer(Modifier.size(50.scaled,24.scaled),"tooltip").tooltipBackground(4.scaled)
        }
    }
    Section("renderButton：普通 / 悬停高亮 / 描边") {
        Row {
            Button(Modifier.size(70.scaled,20.scaled),"button") { TextFlatten { "button".emit() } }
            Spacer(Modifier.width(6.scaled)) {}
            Button(Modifier.size(70.scaled,20.scaled),"button-outline") { TextFlatten { "outline".emit() } }
                .outline(color = Color(255,200,60))
        }
    }
    Section("renderItem：数量 / 耐久 / 附魔光效") {
        Row {
            Item(Modifier.size(24.scaled,24.scaled),"minecraft:diamond",1,id = "item1")
            Item(Modifier.size(24.scaled,24.scaled),"minecraft:stone",64,id = "item2")
            Item(Modifier.size(24.scaled,24.scaled),"minecraft:diamond_pickaxe",1,0.4,id = "item3")
            Item(Modifier.size(24.scaled,24.scaled),"minecraft:diamond_sword",1,null,true,id = "item4")
        }
    }
    Section("文本：flatten（单行）/ foldable（换行）/ autofold（折行）/ 输入框") {
        Column {
            TextFlatten { "flatten：单行，换行符被去掉".emit() }
            TextFoldable { "foldable：第一行\n第二行（\\n 生效）".emit() }
            TextAutoFold(Modifier.width(240.scaled)) {
                "autofold：一段足够长的文字，用来确认自动折行与宽度约束都生效。".emit()
            }
            EditableText(Modifier.size(160.scaled,16.scaled),local { "可编辑文本" }) {}
        }
    }
    Section("字体样式：粗体 / 斜体 / 粗斜 / 下划线 / 删除线 / 混淆 / 阴影 / 全开") {
        Column {
            TextFlatten { "plain 普通".emit() }
            TextFlatten { "bold 粗体".emit(style = DslCharStyle().bold) }
            TextFlatten { "italic 斜体".emit(style = DslCharStyle().italic) }
            TextFlatten { "bold+italic 粗斜".emit(style = DslCharStyle().bold.italic) }
            TextFlatten { "underlined 下划线".emit(style = DslCharStyle().underlined) }
            TextFlatten { "strikeThrough 删除线".emit(style = DslCharStyle().strikeThrough) }
            TextFlatten { "shadowed 阴影".emit(style = DslCharStyle().shadowed) }
            TextFlatten { "obfuscated 混淆".emit(style = DslCharStyle().obfuscated) }
            TextFlatten { "all 全开".emit(style = DslCharStyle().bold.italic.underlined.strikeThrough.obfuscated.shadowed) }
            TextFlatten { "underlined ×2 两倍字号".emit(size = 18.scaled,style = DslCharStyle().underlined) }
            TextFlatten { "strikeThrough ×2 两倍字号".emit(size = 18.scaled,style = DslCharStyle().strikeThrough) }
        }
    }
    Section("字号 / 颜色 / 颜色作用域 / 同行混排 / 非 BMP / 字体名") {
        Column {
            Row {
                // Fixed widths, otherwise the row's weighted aligner stretches each label and the
                // four sizes can no longer be compared.
                TextFlatten(Modifier.width(26.scaled)) { "6".emit(size = 6.scaled) }
                TextFlatten(Modifier.width(26.scaled)) { "9".emit() }
                TextFlatten(Modifier.width(26.scaled)) { "12".emit(size = 12.scaled) }
                TextFlatten(Modifier.width(26.scaled)) { "18".emit(size = 18.scaled) }
            }
            TextFlatten {
                "颜色：".emit()
                "红 ".emit(color = Color(255,80,80))
                "绿 ".emit(color = Color(80,220,80))
                "半透明 ".emit(color = Color(255,255,255,120))
                "默认".emit()
            }
            withColor(Color(255,180,60)) {
                TextFlatten { "withColor 作用域：整段继承橙色".emit() }
            }
            TextFlatten {
                "同行混排：".emit()
                "红粗".emit(color = Color(255,80,80),style = DslCharStyle().bold)
                " + ".emit()
                "蓝斜下划线".emit(color = Color(90,140,255),style = DslCharStyle().italic.underlined)
            }
            TextFlatten { "非 BMP：😀 🧱 ∑ 中文 ①②③".emit() }
            TextFlatten(fontName = "uniform") { "fontName=uniform（后端若忽略它，这行与上一行同形）".emit() }
        }
    }
    Section("裁剪：renderScissor（只裁绘制）/ scissor（裁剪 + 输入门控）") {
        Row {
            Box(Modifier.size(60.scaled,24.scaled),"clip-render") {
                ColorRect(Modifier.size(90.scaled,24.scaled),Color(60,200,60),"clipped")
            }.renderScissor()
            Spacer(Modifier.width(6.scaled)) {}
            Box(Modifier.size(60.scaled,24.scaled),"clip-input") {
                ColorRect(Modifier.size(90.scaled,24.scaled),Color(200,60,200),"clipped-input")
            }.scissor()
        }
    }
    Section("变换：rotate / scale / move / shear / perspective") {
        Row {
            ColorRect(Modifier.size(30.scaled,20.scaled),Color(220,220,60),"t-rotate").rotate(0.35)
            Spacer(Modifier.width(34.scaled)) {}
            ColorRect(Modifier.size(30.scaled,20.scaled),Color(60,220,220),"t-scale").scale(1.4)
            Spacer(Modifier.width(34.scaled)) {}
            ColorRect(Modifier.size(30.scaled,20.scaled),Color(220,60,220),"t-move").move(6.scaled,0.px)
            Spacer(Modifier.width(34.scaled)) {}
            ColorRect(Modifier.size(30.scaled,20.scaled),Color(120,120,255),"t-shear").shear(0.4,0.0)
            Spacer(Modifier.width(34.scaled)) {}
            ColorRect(Modifier.size(30.scaled,20.scaled),Color(255,140,60),"t-persp").perspective(0.002,0.0)
        }
    }
    Section("可选能力：DslBackendTriangles（灰块 = 后端没有这个能力）") {
        PolygonBox()
    }
}.defaultBackground()

/** A labelled block: the label names the capability, the content draws it. */
context(ctx: DslContext)
private fun Section(title: String,content: DslFunction) = Column(Modifier.padding(v = 4.scaled)) {
    TextFlatten { (title + "\n").emit(style = DslCharStyle().shadowed) }
    content()
}

/** A four-corner gradient, through the helper that is written in terms of the quad primitive. */
context(ctx: DslContext)
private fun GradientBox() = Spacer(Modifier.size(50.scaled,24.scaled),"gradient").change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            backend.fillRectGradient(
                instance.rect,
                Color(255,80,80),Color(80,255,80),Color(80,80,255),Color(255,255,80)
            )
            backend.flush()
        }
    }
}

/** The optional triangle-list capability: a fan when it is there, a grey box when it is not. */
context(ctx: DslContext)
private fun PolygonBox() = Spacer(Modifier.size(80.scaled,24.scaled),"triangles").change {
    object : DslComponent by it {
        context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
        override fun <RP> render() {
            val triangles = backend.triangles()
            if(triangles == null) {
                backend.fillRect(instance.rect,Color(90,90,90))
                backend.flush()
                return
            }
            val rect = instance.rect
            triangles.fillPolygon(
                listOf(
                    Position(rect.left + rect.width / 2,rect.top),
                    Position(rect.left,rect.bottom),
                    Position(rect.right,rect.bottom),
                ),
                listOf(Color(255,120,60),Color(60,255,120),Color(120,120,255))
            )
            backend.flush()
        }
    }
}
