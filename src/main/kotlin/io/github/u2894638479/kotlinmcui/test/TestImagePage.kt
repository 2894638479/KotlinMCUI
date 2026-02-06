package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
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
import java.io.File

context(ctx: DslContext)
fun TestImagePage() = Column {
    val colorProp by remember(Color.WHITE).property
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
    val widthProp by 100.remember.property
    val heightProp by 100.remember.property
    val width by widthProp
    val height by heightProp
    Row(Modifier.height(20.scaled)) {
        SliderHorizontal(Modifier,0..500,widthProp) { TextFlatten { "width:$width".emit() } }
        SliderHorizontal(Modifier,0..500,heightProp) { TextFlatten { "height:$height".emit() } }
    }
    val localImageProp by "".remember.property
    val localImage by localImageProp.remap { imageFile(File(it)) }.remember
    val resourceImageProp by "".remember.property
    val resourceWidthProp by 32.px.remember.property
    val resourceHeightProp by 32.px.remember.property
    var resourceWidth by resourceWidthProp
    var resourceHeight by resourceHeightProp
    val resourceImage by resourceImageProp.remap { imageResource(it, resourceWidth, resourceHeight) }.remember
    var imageProp by localImage.remember
    val image by imageProp
    var strategy by ImageStrategy.clip.remember
    Row {
        TextFlatten { "localImage:".emit() }
        EditableText(Modifier.weight(2.0), string = localImageProp) {}
    }.editBoxBackground().clickable { imageProp = localImage }
    Row {
        TextFlatten { "resourceImage: ".emit() }
        EditableText(Modifier.weight(2.0), string = resourceImageProp) {}
    }.editBoxBackground().clickable { imageProp = resourceImage }
    Image(Modifier.size(width.scaled, height.scaled), image, color, strategy) {}
}