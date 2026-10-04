package io.github.u2894638479.kotlinmcui.test

import io.github.u2894638479.kotlinmcui.dsl.DslFunction

/**
 * The page an in-game verification run asks for, by name.
 *
 * The pages live in the framework rather than in a backend: they are written in plain DSL
 * and they check the framework's own contracts — `transform` the pose-matrix third row a
 * backend has to implement, `polygon` the optional triangle capability, and the rest are
 * the framework's demo pages. A backend therefore only drives its platform: wait for a
 * screen, open the page, take the picture, quit.
 *
 * Each page is bound to a `DslFunction` in a property: a lambda literal only picks up the
 * context parameter from an expected type it is written next to, which a `when` branch is
 * not.
 */
private val transformPage: DslFunction = { TestTransformPage() }
private val translatePage: DslFunction = { TestTranslationPage() }
private val polygonPage: DslFunction = { TestPolygonPage() }
private val debugPage: DslFunction = { TestEntry.DebugOverlay.overlay() }
private val testPage: DslFunction = { TestEntry.TestPage.content() }

/** The page [spec] names, falling back to the framework's test page. */
fun verifyPage(spec: String): DslFunction = when (spec) {
    "transform" -> transformPage
    "translate" -> translatePage
    "polygon" -> polygonPage
    "debug" -> debugPage
    else -> testPage
}
