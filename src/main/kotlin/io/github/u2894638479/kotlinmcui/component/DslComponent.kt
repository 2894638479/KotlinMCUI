package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.math.rect.contains
import io.github.u2894638479.kotlinmcui.scope.DslChild


interface DslComponent: DslComponentNavigator, DslComponentAlign, DslComponentEvent, DslComponentMetadata,
    DslIdContext {
    val children: DslChild.List? get() = null
    context(instance: DslComponent)
    fun build() {}
    context(instance: DslComponent)
    fun layoutHorizontal() {}
    context(instance: DslComponent)
    fun layoutVertical() {}

    context(instance: DslComponent)
    override fun <T> testHit(mouse: Position, get: context(DslComponent) (DslComponent) -> T?) =
        if(mouse in instance.rect) get(instance) else null

    context(instance: DslComponent)
    override fun <T> testHit(get: context(DslComponent) (DslComponent) -> T?) = get(instance)

    override val viewHorizontal get() = listOf<List<DslComponent>>()
    override val viewVertical get() = listOf<List<DslComponent>>()
    override val viewSequential get() = listOf<DslComponent>()
}

context(ctx: DslDataStoreContext)
val DslComponent.isFocused get() = focusable && dataStore.focused == identity

context(ctx: DslDataStoreContext)
val DslComponent.isHovered get() = dataStore.hovered == identity

context(ctx: DslDataStoreContext)
val DslComponent.isHighlighted get() = highlightable && (isFocused || isHovered)
