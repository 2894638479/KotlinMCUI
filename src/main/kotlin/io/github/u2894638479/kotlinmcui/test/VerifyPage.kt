package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.entry.DslEntryPage


/** The page [spec] names, falling back to the framework's test page. */
context(ctx: DslContext)
fun verifyPage(spec: String) = when (spec) {
    "transform" -> TestTransformPage()
    "translate" -> TestTranslationPage()
    "polygon" -> TestPolygonPage()
    "color" -> TestColorPage()
    "debug" -> TestEntry.DebugOverlay.overlay()
    "entrypage" -> DslEntryPage()
    else -> TestEntry.TestPage.content()
}
