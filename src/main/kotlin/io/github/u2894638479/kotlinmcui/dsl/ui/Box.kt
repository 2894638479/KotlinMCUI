package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenMaxWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.dsl.remove
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.container.DslChild
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

/**
 * Create a Box layout container.
 *
 * Box has no specific layout direction — its minimum content size is determined
 * by the maximum width and height of its children. It is typically used as a
 * nesting container or base component for other layouts (Row, Column).
 *
 * @param modifier The modifier controlling size, padding, background, etc. Defaults to [Modifier].
 * @param id Unique identifier; if `null`, [function]'s `::class` is used as the ID.
 * @param function DSL builder function executed within an implicit [DslContext].
 * @receiver [DslContext] The implicit DSL context providing component registration and layout.
 * @return The collected component (returned by [collect], typically a [DslScope]).
 * @see DslScopeImpl
 * @see childrenMaxWidth
 * @see childrenMaxHeight
 */
context(ctx: DslContext)
fun Box(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
) = collect(object : DslScope by DslScopeImpl(newChildId(id ?: function::class), modifier, ctx, function) {
    override val contentMinWidth by lazy {
        max(instance.childrenMaxWidth,super.contentMinWidth)
    }

    override val contentMinHeight by lazy {
        max(instance.childrenMaxHeight,super.contentMinHeight)
    }
})

/**
 * Wrap an existing [DslChild] component inside a new Box container.
 *
 * This extension replaces the current component (accessible via [DslChild.currentComponent])
 * with a new Box that preserves the original component's modifier and state.
 * Other children declared in [function] are added first, then the original
 * component is [collect]ed as the last child.
 *
 * @param id Unique identifier for the new Box; defaults to [function]'s `::class` if `null`.
 * @param function DSL builder for the new Box's content.
 * @receiver [DslChild] The component being replaced — must be removed from its parent via [remove].
 * @see DslChild.currentComponent
 * @see remove
 */
context(ctx: DslContext)
fun DslChild.Box(id:Any? = null,function: DslFunction) {
    val component = currentComponent()
    remove(this)
    Box(component.modifier,id = id ?: function::class) {
        function()
        collect(component)
    }
}