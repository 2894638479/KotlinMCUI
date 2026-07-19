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