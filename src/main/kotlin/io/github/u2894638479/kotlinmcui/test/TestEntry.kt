package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.backend.createScreen
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dslBackend
import io.github.u2894638479.kotlinmcui.dslLogger
import io.github.u2894638479.kotlinmcui.entry.DslEntryClient
import io.github.u2894638479.kotlinmcui.entry.DslEntryCommon
import io.github.u2894638479.kotlinmcui.entry.DslEntryGui
import io.github.u2894638479.kotlinmcui.entry.DslEntryOverlay
import io.github.u2894638479.kotlinmcui.entry.DslEntryServer
import io.github.u2894638479.kotlinmcui.entry.DslEntryService
import io.github.u2894638479.kotlinmcui.functions.decorator.background
import io.github.u2894638479.kotlinmcui.functions.decorator.outline
import io.github.u2894638479.kotlinmcui.functions.ui.TextFlatten
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.text.kotlinmcui

object TestEntry: DslEntryService {
    override val name get() = dslBackend.translate(kotlinmcui.testentry.getPath()) ?: "testentry"
    override val id get() = "kotlinmcui"
    override val icon get() = ImageHolder("kotlinmcui:icon.png",16.px,16.px)
    object Common: DslEntryCommon, DslEntryService by TestEntry {
        override fun initialize() {
            dslLogger.info("TestEntry\$Common initialized")
        }
    }
    object Client: DslEntryClient, DslEntryOverlay, DslEntryGui, DslEntryService by TestEntry {
        override val name get() = dslBackend.translate(kotlinmcui.testpage.getPath()) ?: "testpage"
        override fun initializeClient() {
            dslLogger.info("TestEntry\$Client initialized")
        }

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
    object Server: DslEntryServer, DslEntryService by TestEntry {
        override fun initializeServer() {
            dslLogger.info("TestEntry\$Server initialized")
        }
    }
}