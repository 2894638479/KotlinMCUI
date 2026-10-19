package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dsl.dataStore
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.decorator.outline
import io.github.u2894638479.kotlinmcui.dsl.translateGlobal
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.entry.DslEntryGui
import io.github.u2894638479.kotlinmcui.entry.DslEntryOverlay
import io.github.u2894638479.kotlinmcui.image.ImageSource
import io.github.u2894638479.kotlinmcui.entry.DslEntryService
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px

/**
 * The framework's own example entry, declared in `META-INF/kotlinmcui/entries` for every environment.
 *
 * It also covers the two kinds: [TestPage] is a gui page *and* an overlay, [DebugOverlay] is an overlay
 * only. Its `initialize` and `launch` are the reference for the two stages.
 */
object TestEntry : DslEntryService {
    override val name get() = translateGlobal("kotlinmcui.testentry")
    override val id get() = "kotlinmcui"
    override val icon get() = ImageHolder(ImageSource.Resource("kotlinmcui:icon.png"),16.px,16.px)

    override fun initialize() {
        dslLogger.info("TestEntry initialized")
    }

    override fun launch() {
        dslLogger.info("TestEntry launched")
    }

    object TestPage: DslEntryOverlay, DslEntryGui, DslEntryService by TestEntry {
        override val id get() = "kotlinmcui:testpage"
        override val name get() = translateGlobal("kotlinmcui.testpage")

        context(ctx: DslContext)
        override fun gui() {
            TestPage()
        }

        context(ctx: DslContext)
        override fun overlay() {
            TextFlatten {
                "Test Overlay".emit(size = 18.scaled)
            }.background(Color(100, 100, 100, 100)).outline()
        }
    }

    object DebugOverlay: DslEntryOverlay, DslEntryService by TestEntry {
        override val id get() = "kotlinmcui:debugoverlay"
        override val name get() = translateGlobal("kotlinmcui.debugoverlay")
        context(ctx: DslContext)
        override fun overlay() {
            val screen = dataStore.root.children.getOrNull(1) as? DslScreen
            val debugOverlay = screen?.dataStore?.debugOverlay ?: dataStore.debugOverlay
            debugOverlay.content()
        }
    }
}
