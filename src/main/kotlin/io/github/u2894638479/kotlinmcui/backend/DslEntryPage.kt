package io.github.u2894638479.kotlinmcui.backend

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.highlightBox
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.text.DslCharStyle

context(ctx: DslContext)
fun DslEntryPage() = Row {
    val scrollerProp by Scroller.empty.remember.property
    val scrollProp by 0.0.remember.property
    Spacer(Modifier.width(10.scaled)) {}
    LazyColumn(Modifier,scrollerProp,scrollProp) {
        DslEntryService.services.forEachWithId {
            var errStr by remember<String?>(null)
            Row(Modifier.padding(5.scaled)) {
                Image(Modifier.size(40.scaled,40.scaled),it.icon) {}
                Column {
                    TextAutoFold { it.name.emit(size = 18.scaled, style = DslCharStyle().shadowed) }
                    TextAutoFold { it.id.emit(Color(150,150,150), style = DslCharStyle().italic.shadowed) }
                    errStr?.let {
                        TextAutoFold { it.emit(Color.RED) }
                    }
                }
            }.highlightBox(-2.scaled,3.scaled).clickable {
                it.createScreen()?.show() ?: run {
                    errStr = "no screen registered"
                }
            }
        }
    }
    ScrollBarVertical(Modifier.width(10.scaled),scrollerProp) {}
    Spacer(Modifier.width(10.scaled)) {}
}.defaultBackground()