package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslRootRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * Create an item rendering component to display a Minecraft-style item icon.
 *
 * Renders via the backend's [DslRootRenderer.renderItem]. Supports item icon,
 * stack count, durability bar, and enchantment glint effect.
 *
 * @param item The item identifier (e.g. Minecraft namespace ID like `"minecraft:diamond"`).
 * @param count Stack size displayed at the bottom-right of the icon.
 * @param damage Durability fraction (0.0 = full, 1.0 = broken), or `null` to hide.
 * @param enchanted Whether to show the enchantment glint effect. Defaults to `false`.
 * @param id Unique identifier for component distinction and reference.
 * @receiver [DslContext] The implicit DSL context.
 * @see DslRootRenderer
 * @see DslComponentImpl
 */
context(ctx: DslContext)
fun Item(
    modifier: Modifier = Modifier,
    item: String,
    count: Int,
    damage: Double? = null,
    enchanted:Boolean = false,
    id:Any
) = collect(object : DslComponent by DslComponentImpl(newChildId(id),modifier,ctx) {
    context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderItem(instance.rect,item,count,damage,enchanted)
        backend.flush()
    }
})