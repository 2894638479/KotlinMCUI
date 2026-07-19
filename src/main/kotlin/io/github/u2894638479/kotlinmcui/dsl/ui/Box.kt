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
 * 创建一个 Box 布局容器。
 *
 * Box 是一种无特定排列方向的布局容器，其内容的最小尺寸由所有子组件的最大宽度和最大高度决定。
 * 它通常作为其他布局（如 Row、Column）的嵌套容器或基础组件。
 *
 * @param modifier 修饰器，用于控制组件的尺寸、边距、背景等属性，默认为 [Modifier]。
 * @param id 组件的唯一标识符；若为 `null`，则使用 [function] 的 `::class` 作为 ID。
 * @param function DSL 构建函数，用于在 Box 内声明子组件。该函数在 [DslContext] 的隐式上下文中执行。
 * @receiver [DslContext] 隐式 DSL 上下文，提供组件注册和布局计算能力。
 * @return 收集到的组件对象（具体类型由 [collect] 返回，通常为 [DslScope] 或等效的容器引用）。
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
 * 将当前 [DslChild] 组件替换为一个新的 Box 容器，并将原组件作为子项插入到 Box 中。
 *
 * 此扩展函数允许将一个已存在的组件（通过 [currentComponent] 获取）包裹到 Box 中，
 * 同时保留原有组件的修饰器和状态。调用顺序为：先执行 [function] 中声明的其他子组件，
 * 再在最后将原组件通过 [collect] 添加到 Box 中。
 *
 * @param id 新 Box 的唯一标识符，若为 `null` 则使用 [function] 的 `::class` 作为 ID。
 * @param function DSL 构建函数，用于在新 Box 内定义其他布局或组件。
 * @receiver [DslChild] 代表当前正在渲染的组件，必须通过 [remove] 将其从原有父容器中移除。
 * @see DslChild#currentComponent
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