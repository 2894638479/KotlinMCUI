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
import io.github.u2894638479.kotlinmcui.dsl.translate
import io.github.u2894638479.kotlinmcui.dsl.ui.*
import io.github.u2894638479.kotlinmcui.backend.Environment
import io.github.u2894638479.kotlinmcui.math.Color
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
import io.github.u2894638479.kotlinmcui.scroll.Scroller
import io.github.u2894638479.kotlinmcui.text.DslCharStyle
import io.github.u2894638479.kotlinmcui.widget.Editors
import io.github.u2894638479.kotlinmcui.widget.Presets
import io.github.u2894638479.kotlinmcui.widget.Presets.styledTooltip

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
            Editors.BoolButton(showCodeEntry,translate("kotlinmcui.entrypage.show_code_entry")) {}
            Editors.BoolButton(showDetails,translate("kotlinmcui.entrypage.show_details")) {}
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
                        flags.environments.forEach { env -> " ${env.name.lowercase()} ".emit(environmentColor(env)) }
                        if(flags.isGui) " gui ".emit(Color(150,150,240))
                        if(flags.isOverlay) " overlay ".emit(Color(200,200,20))
                    }
                    errStr?.let { TextAutoFold { it.emit(Color.RED) } }
                }
                if(flags.isOverlay) Column(Modifier.weight(0.0)) {
                    it as DslEntryOverlay
                    val prop = object: StableRW<Boolean> {
                        override fun getValue() = DslEntryLoader.isOverlayEnabled(it)
                        override fun setValue(value: Boolean) { DslEntryLoader.setOverlayEnabled(it,value) }
                    }
                    Editors.BoolButton(prop,translate("kotlinmcui.entrypage.overlay")) {}
                    if(prop.getValue()) {
                        Presets.Button("bring to top",DslEntryLoader.enabledOverlays.firstOrNull() !== it) {
                            DslEntryLoader.bringOverlayToTop(it)
                        }
                    }
                }
            }.hoverMask().highlightBox(-2.scaled,3.scaled).clickable(flags.isGui) {
                showScreen(it.name) {
                    (it as DslEntryGui).gui()
                }
            }.styledTooltip {
                if(!showDetails.value) {
                    TextFlatten { it.name.emit() }
                    return@styledTooltip
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
                        flags.environments.forEach { env -> enter().also { env.name.lowercase().emit(environmentColor(env)) } }
                        if(flags.isGui) enter().also { translate("kotlinmcui.entrypage.tooltip.gui").emit(Color(150,150,240)) }
                        if(flags.isOverlay) enter().also { translate("kotlinmcui.entrypage.tooltip.overlay").emit(Color(200,200,20)) }
                    }
                }
            }
        }

        // Listings that could not be loaded: no instance, so no icon and no controls. One compact
        // line each, appended after the working entries and never hidden by the code-entry toggle
        // - a listing that fails is exactly what has to stay visible.
        DslEntryLoader.brokenEntries.forEachWithId { broken ->
            Row(Modifier.padding(2.scaled)) {
                TextAutoFold {
                    "! ".emit(Color(230,80,80))
                    broken.className.emit(Color(200,140,140))
                    "  ".emit()
                    broken.hookName.emit(Color(150,150,150))
                    "  ".emit()
                    broken.reason.emit(Color(200,200,200))
                }
            }.styledTooltip {
                TextAutoFold {
                    broken.className.emit()
                    enter()
                    broken.reason.emit()
                }
            }
        }
    }
    ScrollBarVertical(Modifier.width(10.scaled),scrollerProp) {}
    Spacer(Modifier.width(10.scaled)) {}
}.defaultBackground()
/** The badge colour per environment; one colour per side, so the list reads at a glance. */
private fun environmentColor(environment: Environment) = when(environment) {
    Environment.CLIENT -> Color(30,200,30)
    Environment.SERVER -> Color(200,30,30)
    Environment.STANDALONE -> Color(200,200,200)
}
