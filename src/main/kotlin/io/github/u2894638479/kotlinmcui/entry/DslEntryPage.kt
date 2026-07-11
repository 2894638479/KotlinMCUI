package io.github.u2894638479.kotlinmcui.entry

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.decorator.clickable
import io.github.u2894638479.kotlinmcui.dsl.decorator.highlightBox
import io.github.u2894638479.kotlinmcui.dsl.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.dsl.forEachWithId
import io.github.u2894638479.kotlinmcui.dsl.local
import io.github.u2894638479.kotlinmcui.dsl.showScreen
import io.github.u2894638479.kotlinmcui.dsl.static
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.align.Align
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.StableRW
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.prop.value
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.text.kotlinmcui
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip

context(ctx: DslContext)
fun DslEntryPage() = Row {
    val scrollerProp = static { Scroller.empty }
    val scrollProp = static.animatable { 0.0 }
    Spacer(Modifier.width(10.scaled)) {}
    LazyColumn(Modifier,scrollerProp,scrollProp) {
        val showCodeEntry = local { false }
        val showDetails = local { false }
        Row {
            Spacer(Modifier weight Double.MAX_VALUE) {}
            Config.BoolButton(showCodeEntry,kotlinmcui.entrypage.show_code_entry()) {}
            Config.BoolButton(showDetails,kotlinmcui.entrypage.show_details()) {}
        }
        DslEntryLoader.entries.forEachWithId { (it, flags) ->
            if(!showCodeEntry.value && !flags.isGui && !flags.isOverlay) return@forEachWithId
            var errStr by local<String?> { null }
            Row(Modifier.padding(5.scaled)) {
                Image(Modifier.size(40.scaled,40.scaled),it.icon) {}
                Column {
                    TextAutoFold { it.name.emit(size = 18.scaled, style = DslCharStyle().shadowed) }
                    TextAutoFold { it.id.emit(Color(150,150,150), style = DslCharStyle().italic.shadowed) }
                    TextAutoFold {
                        if(flags.isServer) " server ".emit(Color(200,30,30))
                        if(flags.isClient) " client ".emit(Color(30,200,30))
                        if(flags.isCommon) " common ".emit(Color(200,200,200))
                        if(flags.isGui) " gui ".emit(Color(150,150,240))
                        if(flags.isOverlay) " overlay ".emit(Color(200,200,20))
                    }
                    errStr?.let { TextAutoFold { it.emit(Color.RED) } }
                }
                if(flags.isOverlay) Column(Modifier.weight(0.0)) {
                    it as DslEntryOverlay
                    val list = DslEntryLoader.enabledOverlays
                    val prop = object: StableRW<Boolean> {
                        override fun getValue() = it in list
                        override fun setValue(value: Boolean) { if(!list.remove(it)) list += it }
                    }
                    Config.BoolButton(prop,kotlinmcui.entrypage.overlay()) {}
                    if(prop.getValue()) {
                        Simple.Button("bring to top",list.getOrNull(0) != it) {
                            list.remove(it)
                            list.addFirst(it)
                        }
                    }
                }
            }.hoverMask().highlightBox(-2.scaled,3.scaled).clickable(flags.isGui) {
                showScreen(it.name) {
                    (it as DslEntryGui).content()
                }
            }.simpleTooltip {
                if(!showDetails.value) {
                    TextFlatten { it.name.emit() }
                    return@simpleTooltip
                }
                val className by static.cached(it) {
                    it.javaClass.canonicalName?.removePrefix(it.javaClass.`package`?.name?.plus(".") ?: "")
                }
                val packageName by static.cached(it) {
                    it.javaClass.`package`.name ?: "unknown package"
                }
                Column {
                    Row {
                        Image(Modifier.size(64.scaled,64.scaled),it.icon) {}
                        Column {
                            TextAutoFold(Modifier padding 5.scaled) { it.name.emit() }
                            TextAutoFold {
                                "package: ".emit(Color(20,20,200))
                                packageName.emit()
                                enter()
                                "class: ".emit(Color(20,20,200))
                                className?.emit()
                            }
                        }
                    }
                    TextAutoFold(Modifier.width(200.scaled),horizontalAligner = Aligner.close(Align.LOW)) {
                        val key = kotlinmcui.entrypage.tooltip
                        if(flags.isServer) enter().also { key.server().emit(Color(200,30,30)) }
                        if(flags.isClient) enter().also { key.client().emit(Color(30,200,30)) }
                        if(flags.isCommon) enter().also { key.common().emit(Color(200,200,200)) }
                        if(flags.isGui) enter().also { key.gui().emit(Color(150,150,240)) }
                        if(flags.isOverlay) enter().also { key.overlay().emit(Color(200,200,20)) }
                    }
                }
            }
        }
    }
    ScrollBarVertical(Modifier.width(10.scaled),scrollerProp) {}
    Spacer(Modifier.width(10.scaled)) {}
}.defaultBackground()