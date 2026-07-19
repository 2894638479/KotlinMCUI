package io.github.u2894638479.kotlinmcui.dsl.ui

import io.github.u2894638479.kotlinmcui.component.childrenMaxHeight
import io.github.u2894638479.kotlinmcui.component.childrenSumWidth
import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.dsl.DslFunction
import io.github.u2894638479.kotlinmcui.dsl.collect
import io.github.u2894638479.kotlinmcui.dsl.newChildId
import io.github.u2894638479.kotlinmcui.math.Measure.Companion.max
import io.github.u2894638479.kotlinmcui.math.align.Aligner
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.prop.mapView
import io.github.u2894638479.kotlinmcui.container.DslScope
import io.github.u2894638479.kotlinmcui.container.DslScopeImpl

/**
 * 创建一个水平布局容器（Row）。
 *
 * Row 将所有子组件沿水平方向排列，支持自定义水平对齐策略。
 * 其 `contentMinWidth` 取所有子组件宽度之和（[childrenSumWidth]）与现有约束的最大值，
 * `contentMinHeight` 取所有子组件最大高度（[childrenMaxHeight]）与现有约束的最大值。
 *
 * 重写了 [DslScope] 的 `viewHorizontal` 和 `viewVertical` 属性：
 * - `viewHorizontal` 返回每个子组件单独构成一行视图（便于水平方向的分组测量）；
 * - `viewVertical` 返回所有子组件作为单一列视图（垂直方向无分隔）。
 *
 * @param modifier 修饰器，用于控制组件尺寸、边距、背景等属性，默认为 [Modifier]。
 * @param alignerHorizontal 水平对齐器，负责决定子组件在横向上的分布与尺寸分配策略。
 *                          默认值为 [Aligner.weightedStrictByMin]，表示按子组件的最小尺寸进行严格加权分配。
 * @param id 组件的唯一标识符；若为 `null`，则使用 [function] 的 `::class` 作为 ID。
 * @param function DSL 构建函数，用于在 Row 内声明子组件。
 * @receiver [DslContext] 隐式 DSL 上下文。
 * @return 收集到的组件对象（具体类型由 [collect] 返回）。
 * @see DslScopeImpl
 * @see childrenSumWidth
 * @see childrenMaxHeight
 * @see Aligner
 */
context(ctx: DslContext)
fun Row(
    modifier: Modifier = Modifier,
    alignerHorizontal: Aligner = Aligner.weightedStrictByMin,
    id:Any? = null,
    function: DslFunction
) = collect(
    object : DslScope by DslScopeImpl(
        newChildId(id ?: function::class),
        modifier,
        ctx,
        function,
        alignerHorizontal = alignerHorizontal
    ) {
        override val contentMinWidth by lazy {
            max(instance.childrenSumWidth,super.contentMinWidth)
        }

        override val contentMinHeight by lazy {
            max(instance.childrenMaxHeight,super.contentMinHeight)
        }

        override val viewHorizontal get() = children.mapView { listOf(it) }
        override val viewVertical get() = listOf(children)
    }
)