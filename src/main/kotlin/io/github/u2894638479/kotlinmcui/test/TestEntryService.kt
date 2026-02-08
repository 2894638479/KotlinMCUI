package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.backend.DslEntryService
import io.github.u2894638479.kotlinmcui.backend.createScreen
import io.github.u2894638479.kotlinmcui.dslBackend
import io.github.u2894638479.kotlinmcui.dslLogger
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.px

class TestEntryService: DslEntryService {
    override val name get() = dslBackend.translate("kotlinmcui.testpage") ?: "testpage"
    override val id get() = "kotlinmcui"
    override val icon get() = ImageHolder("kotlinmcui:icon.png",16.px,16.px)
    override fun initialize() {
        dslLogger.info("TestEntry initialized")
    }
    override fun createScreen() = dslBackend.createScreen(name) { TestPage() }
}