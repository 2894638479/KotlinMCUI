package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.onFilesDropped
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.imageFile
import io.github.u2894638479.kotlinmcui.functions.imageResource
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.image.ImageStrategy
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.*
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.remap
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.utils.Config
import java.io.File

private enum class Mode{ LOCAL,RESOURCE }

context(ctx: DslContext)
fun TestImagePage() = ScrollableColumn {
    val colorProp by remember(Color.WHITE).property
    val color by colorProp
    val widthProp by 100.remember.property
    val heightProp by 100.remember.property
    val width by widthProp
    val height by heightProp
    val localImageProp by "".remember.property
    val localImage by localImageProp.remap { imageFile(File(it)) }
    val resourceImageProp by "minecraft:textures/block/dirt.png".remember.property
    val resourceWidthProp by 32.px.remember.property
    val resourceHeightProp by 32.px.remember.property
    val resourceImage by resourceImageProp.remap { imageResource(it, resourceWidthProp.value,resourceHeightProp.value) }
    var strategy by ImageStrategy.clip.remember

    val mode by Mode.LOCAL.remember.property

    Config.ColorEdit(colorProp) {}
    Row {
        Config.IntSlider(widthProp,0..500,"width") {}
        Config.IntSlider(heightProp,0..500,"height") {}
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