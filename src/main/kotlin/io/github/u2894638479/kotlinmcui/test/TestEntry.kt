package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.container.DslScreen
import io.github.u2894638479.kotlinmcui.container.topComponent
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.entry.*
import io.github.u2894638479.kotlinmcui.dsl.dataStore
import io.github.u2894638479.kotlinmcui.dsl.decorator.background
import io.github.u2894638479.kotlinmcui.dsl.decorator.outline
import io.github.u2894638479.kotlinmcui.dsl.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.logger.dslLogger
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.text.kotlinmcui

object TestEntry: DslEntryService {
    override val name get() = kotlinmcui.testentry()
    override val id get() = "kotlinmcui"
    override val icon get() = ImageHolder("kotlinmcui:icon.png",16.px,16.px)
    object Common: DslEntryCommon, DslEntryService by TestEntry {
        override fun initialize() {
            dslLogger.info("TestEntry\$Common initialized")
        }
    }
    object TestPage: DslEntryOverlay, DslEntryGui, DslEntryService by TestEntry {
        override val name get() = kotlinmcui.testpage()
        context(ctx: DslContext)
        override fun content() {
            TestPage()
        }

        context(ctx: DslContext)
        override fun overlay() {
            TextFlatten {
                "Test Overlay".emit(size = 18.scaled)
            }.background(Color(100, 100, 100, 100)).outline()
        }
    }
    object Client: DslEntryClient, DslEntryService by TestEntry {
        override fun initializeClient() {
            dslLogger.info("TestEntry\$Client initialized")
        }

    }
    object Server: DslEntryServer, DslEntryService by TestEntry {
        override fun initializeServer() {
            dslLogger.info("TestEntry\$Server initialized")
        }
    }
    object DebugOverlay: DslEntryOverlay, DslEntryService by TestEntry {
        override val name get() = kotlinmcui.debugoverlay()
        context(ctx: DslContext)
        override fun overlay() {
            val screen = topComponent.children.getOrNull(1) as? DslScreen
            val debugOverlay = screen?.dataStore?.debugOverlay ?: dataStore.debugOverlay
            debugOverlay.content()
        }
    }
}