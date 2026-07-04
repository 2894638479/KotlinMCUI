package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.decorator.onFilesDropped
import io.github.u2894638479.kotlinmcui.functions.imageFile
import io.github.u2894638479.kotlinmcui.functions.imageResource
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.ui.Image
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.functions.ui.ScrollableColumn
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.utils.Config
import java.io.File

private enum class Mode{ LOCAL,RESOURCE }

context(ctx: DslContext)
fun TestImagePage() = ScrollableColumn {
    val colorProp = local { Color.WHITE }
    val color by colorProp
    val widthProp = local { 100 }
    val heightProp = local { 100 }
    val width by widthProp
    val height by heightProp
    val localImageProp = local { "" }
    val localImage by localImageProp.remap { imageFile(File(it)) }
    val resourceImageProp = local { "minecraft:textures/block/dirt.png" }
    val resourceWidthProp = local { 32.px }
    val resourceHeightProp = local { 32.px }
    val resourceImage by resourceImageProp.remap { imageResource(it, resourceWidthProp.value,resourceHeightProp.value) }
    var strategy by local { ImageStrategy.clip }

    val mode = local { Mode.LOCAL }

    Config.ColorEdit(colorProp) {}
    Row {
        Config.Slider(widthProp,0..500,"width") {}
        Config.Slider(heightProp,0..500,"height") {}
    }
    Config.EnumButton(mode,"mode") {}

    when(mode.value) {
        Mode.LOCAL -> {
            Config.String(localImageProp,"localImage") {}
            TextFlatten(Modifier.height(30.scaled)) { "drop files here".emit() }.hoverMask()
                .onFilesDropped { localImageProp.value = it.firstOrNull()?.toString() ?: "" }
            Image(Modifier.size(width.scaled, height.scaled), localImage, color, strategy) {}
        }
        Mode.RESOURCE -> {
            Config.String(resourceImageProp,"resource") {}
            Image(Modifier.size(width.scaled, height.scaled), resourceImage, color, strategy) {}
        }
    }
}