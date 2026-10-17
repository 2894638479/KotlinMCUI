package io.github.u2894638479.kotlinmcui.prop

import io.github.u2894638479.kotlinmcui.container.DslRoot
import io.github.u2894638479.kotlinmcui.dsl.stable
import io.github.u2894638479.kotlinmcui.dsl.ui.Spacer
import io.github.u2894638479.kotlinmcui.identity.DslId
import io.github.u2894638479.kotlinmcui.integration.TestBackend
import io.github.u2894638479.kotlinmcui.integration.TestRoot
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The `key` parameter of the property scopes, and what happens when two declaration sites try to use
 * one key.
 *
 * A key is a sub-key inside one declaration site: the site itself is part of the storage slot, so the
 * "same key, different site" case is at worst a suspicious pattern, not a shared slot. It becomes
 * observable through [PropertyLifeScope.reusedKeys], which the debug overlay reads.
 */
class PropertyKeyTest {

    private fun render(root: DslRoot, screen: io.github.u2894638479.kotlinmcui.container.DslScreen) =
        kotlin.context(TestBackend, Unit, Position(0.px, 0.px)) { screen.render<Unit>() }

    private fun pageWith(tag: String, body: io.github.u2894638479.kotlinmcui.dsl.DslFunction) =
        TestBackend.create(tag, body).dslScreen

    @Test
    fun `two declaration sites using one key are recorded`() {
        val root = TestRoot.newRoot()
        val screen = pageWith("dup") {
            // Two distinct lambdas -> two distinct declaration sites, one key.
            val first = stable.property(key = "shared") { 1 }
            val second = stable.property(key = "shared") { "two".length }
            Spacer(Modifier.size((first.value + second.value).px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(root, screen)

        val recorded = screen.dataStore.stableLifeScope.reusedKeys
        assertEquals(1, recorded.size, "one key claimed by two sites must be recorded exactly once")
        assertEquals(DslId("shared"), recorded.single().key)
    }

    @Test
    fun `one declaration site using the same key every frame is not recorded`() {
        val root = TestRoot.newRoot()
        val screen = pageWith("single") {
            val only = stable.property(key = "only") { 1 }
            Spacer(Modifier.size(only.value.px, 10.px), "spacer")
        }
        root.children.collect(screen)
        render(root, screen)
        render(root, screen)

        assertTrue(
            screen.dataStore.stableLifeScope.reusedKeys.isEmpty(),
            "a page declaring its own key on every frame is the normal case",
        )
    }
}
