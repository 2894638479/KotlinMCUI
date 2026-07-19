package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.backend.DslBackendRenderer
import io.github.u2894638479.kotlinmcui.component.DslComponent
import io.github.u2894638479.kotlinmcui.component.DslComponentImpl
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Position
import io.github.u2894638479.kotlinmcui.modifier.Modifier

/**
 * 创建一个物品渲染组件，用于在 UI 中显示 Minecraft 风格的物品图标。
 *
 * 该组件依赖于后端渲染器 [DslBackendRenderer] 的具体实现来完成实际的绘制工作。
 * 支持显示物品图标、数量（count）、耐久度损耗（damage）以及附魔光效（enchanted）。
 *
 * @param modifier 修饰器，用于控制组件的尺寸、位置等属性，默认为 [Modifier]。
 * @param item 物品的唯一标识字符串（例如 Minecraft 中的命名空间 ID，如 `"minecraft:diamond"`）。
 * @param count 物品堆叠数量，将在图标右下角显示。
 * @param damage 物品的耐久度损耗比例，`null` 表示不显示耐久度条。
 * @param enchanted 是否启用附魔光效（闪烁的紫色光芒），默认为 `false`。
 * @param id 组件的唯一标识符（必填，无默认值），用于组件间的区分与引用。
 * @receiver [DslContext] 隐式 DSL 上下文。
 * @return 收集到的组件对象（具体类型由 [collect] 返回）。
 * @see DslBackendRenderer
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
    context(backend: DslBackendRenderer<RP>, renderParam: RP, mouse: Position)
    override fun <RP> render() {
        backend.renderItem(instance.rect,item,count,damage,enchanted)
        backend.flush()
    }
})