package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.highlightBox
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.local
import io.github.u2894638479.kotlinmcui.functions.showScreen
import io.github.u2894638479.kotlinmcui.functions.static
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.height
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple

context(ctx: DslContext)
fun DslEntryPage() = Row {
    val scrollerProp = static { Scroller.empty }
    val scrollProp = static { 0.0 }
    Spacer(Modifier.width(10.scaled)) {}
    LazyColumn(Modifier,scrollerProp,scrollProp) {
        DslEntryLoader.entries.forEachWithId {
            var errStr by local<String?> { null }
            Row(Modifier.padding(5.scaled)) {
                Image(Modifier.size(40.scaled,40.scaled),it.icon) {}
                Column {
                    TextAutoFold { it.name.emit(size = 18.scaled, style = DslCharStyle().shadowed) }
                    TextAutoFold { it.id.emit(Color(150,150,150), style = DslCharStyle().italic.shadowed) }
                    TextAutoFold {
                        if(it is DslEntryServer) " server ".emit(Color(200,30,30))
                        if(it is DslEntryClient) " client ".emit(Color(30,200,30))
                        if(it is DslEntryCommon) " common ".emit(Color(200,200,200))
                        if(it is DslEntryGui) " gui ".emit(Color(150,150,240))
                        if(it is DslEntryOverlay) " overlay ".emit(Color(200,200,20))
                    }
                    errStr?.let { TextAutoFold { it.emit(Color.RED) } }
                }
                if(it is DslEntryOverlay) Column(Modifier.weight(0.0)) {
                    val list = DslEntryLoader.enabledOverlays
                    val prop = object: StableRW<Boolean> {
                        override fun getValue() = it in list
                        override fun setValue(value: Boolean) { if(!list.remove(it)) list += it }
                    }
                    Config.BoolButton(prop,"overlay") {}
                    if(prop.getValue()) {
                        Simple.Button("set as top",list.getOrNull(0) != it) {
                            list.remove(it)
                            list.addFirst(it)
                        }
                    }
                }
            }.hoverMask().highlightBox(-2.scaled,3.scaled).clickable(it is DslEntryGui) {
                showScreen {
                    (it as DslEntryGui).content()
                }
            }
        }
    }
    ScrollBarVertical(Modifier.width(10.scaled),scrollerProp) {}
    Spacer(Modifier.width(10.scaled)) {}
}.defaultBackground()