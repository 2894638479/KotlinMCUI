package io.github.u2894638479.kotlinmcui.component

import io.github.u2894638479.kotlinmcui.context.DslDataStoreContext
import io.github.u2894638479.kotlinmcui.context.DslIdContext
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.math.Position


interface DslComponent: DslComponentNavigator, DslComponentAlign, DslComponentEvent, DslComponentMetadata,
    DslIdContext {
    context(instance: DslComponent)
    fun build() {}
    context(instance: DslComponent)
    fun clear() {}
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

context(ctx: DslDataStoreContext, instance: DslComponent)
val isFocused get() = instance.focusable && dataStore.focused == instance.identity

context(ctx: DslDataStoreContext, instance: DslComponent)
val isHovered get() = dataStore.hovered == instance.identity

context(ctx: DslDataStoreContext, instance: DslComponent)
val isHighlighted get() = instance.highlightable && (isFocused || isHovered)
