---
title: 容器与内建组件
nav_order: 5
parent: Dev
---

# 容器与内建组件
`dsl/ui` 里的组件全部由公开 DSL 拼装而成，没有额外的内部特权。它们的差异集中在三件事：**用哪个 aligner**、**上报多大的最小尺寸**、**是不是一个事件容器**。

## 布局容器
| 组件 | 水平 aligner | 垂直 aligner | `contentMinWidth` | `contentMinHeight` |
| --- | --- | --- | --- | --- |
| `Box` | `simplePlace` | `simplePlace` | 子节点最大宽度与自身声明取大 | 子节点最大高度与自身声明取大 |
| `Row` | `weightedStrictByMin` | `simplePlace` | 子节点宽度之和 | 子节点最大高度 |
| `Column` | `simplePlace` | `weightedStrictByMin` | 子节点最大宽度 | 子节点高度之和 |

`Box` 的测量值是"取大"而不是"求和"，因为它的子节点是层叠的，彼此不争夺空间。`Row` / `Column` 则必须在主轴方向求和——这正是"父容器会被子组件撑开"的来源。

`Row` 与 `Column` 都允许覆写主轴 aligner，因此可以做出"末尾对齐""等距分布"等变体，而不需要新的容器类型。

## 三个辅助容器
### `Spacer`
`Spacer` 是"什么都不画"的占位容器。它的价值在于让"剩余空间"变成一个可以被 `weight` 争夺的对象：

```kotlin
Row {
    Button { }
    Spacer(Modifier.weight(Double.MAX_VALUE))
    Button { }
}
```

因为没有内容，它的最小尺寸是 0，可以让出全部空间。它没有 body 参数，`id` 是必填项——按惯例调用点写 `Spacer(...) { }`，那个空 lambda 落在 `id` 槽里充当声明位置标识。

### `ShrinkBox`
`ShrinkBox` 是 `Box` 加两个装饰器的组合：让自身上报尺寸归零（`shrink`），并裁剪绘制（`scissor`）。

用途是"内容比容器大时不要撑大容器"。因为上报尺寸归零，父容器不会为它预留空间，实际大小完全由父容器分配；超出的部分被裁掉。注意 `shrink` 只改变**对外上报**的尺寸，`Box` 内部对子节点的测量仍然照常。

### `LateBox`
`LateBox` 解决"必须在知道自己被分配了多少空间之后，才能决定构建什么内容"的问题——典型场景是内容需要按宽度换行或按宽度选择布局。

它的实现很直接：把 `build()` 与横向布局都留空，等到 `layoutVertical()` 时才

1. 在自己的矩形内执行内容函数（内容函数的接收者是一个 `Rect`，可以直接读宽高）；
2. 给子节点挂载并递归 `build()`；
3. 用 `simplePlace` 依次做横向与纵向对齐。

代价有两点：一是**打破了"先横后纵"的约定**（它是唯一例外），二是**子节点的最小尺寸无法再撑大 `LateBox` 本身**——因为尺寸已经确定，子节点只能被塞进已有空间。

它的垂直 aligner 是硬编码的，不可配置。

由于内容函数在纵向布局里执行，它用一个帧戳保证**同一帧内的第二次布局是空操作**：某些父容器（例如惰性列表在测量条目尺寸时）会在一帧内多次触发布局，若不设防，内容函数会再执行一遍，把同一批子节点重复注册进去。

## Overlay
`Overlay` 与其它容器最根本的差别是：它**不注册到父节点的子节点列表**，而是注册到上下文里的一个独立的 `overlays` 收集槽。

这带来两个结果：

- 它可以脱离父节点的布局约束，自由定位到屏幕任意位置；
- 它由 Screen 单独管理，支持"构建过程中又产生新的 Overlay"这种迭代嵌套。

Screen 的构建分三个阶段：主树 → 循环处理 Overlay 批次（每一轮都可能产出新批次）→ 提示层。最终子节点列表被重排为 `提示层 + Overlay 层 + 主树`，配合逆序绘制得到 z 序 `提示层 > Overlay 层 > 主树`。

`Overlay` 的定位完全交给 `OverlayAlign`，由它在横纵两个方向分别计算区间。

### `OverlayAlign` 的六种策略
| 策略 | 定位方式 |
| --- | --- |
| `FixRect(rect)` | 直接使用给定矩形（两个轴都原样复制） |
| `InsideRect(rect)` | 放在给定矩形**内部**，按自身 `align` 贴边或居中，尺寸取自身最小尺寸 |
| `OutsideRect(rect)` | 放在给定矩形**外部**，按自身 `align` 贴边 |
| `ToPoint(point)` | 以给定点为基准按自身 `align` 摆放 |
| `AutoToPoint(point, keepin)` | 跟随一个点，若超出 `keepin` 则自动翻到另一侧，再不行则贴边 |
| `AutoToRect(rect, keepin)` | 贴在给定矩形下方，若下方放不下则翻到上方 |

`AutoToPoint` 的两个轴偏好不同：水平方向优先向右、放不下则向左、仍放不下就贴右边界；垂直方向优先向上、放不下则向下、仍放不下贴下边界。

`AutoToRect` 更简单：水平方向从锚点左边界开始，超出右边界就贴右边界；垂直方向从锚点下边界开始，超出下边界就翻到锚点上边界之上（翻转后不再做上边界钳制）。

`DslChild.Overlay` 重载是常用封装：它以当前组件的矩形作为 `FixRect`，也就是"把这个组件的副本画到同一个位置"。

### `MouseTip`
`MouseTip` 就是 `Overlay` 加 `ToPoint(鼠标位置)`。它不跟随屏幕边界——超出屏幕时不会自动翻转，需要的话应当用 `AutoToPoint`。

### 提示层
Screen 每帧自动构建两层 Overlay：

- **IME 预编辑浮层**：找到当前焦点组件，取其 `preeditArea` 经 `relativeTransform` 变换到屏幕空间，在 `AutoToRect` 定位下画一个白底绿框的浮层显示组合文本。该浮层的装饰器在渲染时把自己最终的矩形回报给后端，作为平台输入法候选窗口的位置。
- **Tooltip**：优先取鼠标位置命中组件的 `tooltip`，否则取焦点组件的 `tooltip`。鼠标悬停时用 `AutoToPoint` 跟随鼠标；键盘焦点（鼠标不在组件上）时用 `AutoToRect` 锚在焦点组件上。

组件声明了 `tooltip` 并不会自动显示——必须存在一个 `Tooltip{}` 来承载它。

`selfRenderPreedit` 为真的组件会自行绘制组合文本，此时框架不再画那个浮层，而改为在派发 `globalPreedit` 时把组件的 `preeditArea` 上报给后端。两条路径最终都会设置候选窗口位置。

## 叶子组件
| 组件 | 说明 |
| --- | --- |
| `ColorRect` | 纯色矩形，没有 body 参数 |
| `DefaultBackground` | 铺一层原版风格的界面背景，通常放在 Screen 内容最后 |
| `TextFlatten` / `TextFoldable` / `TextAutoFold` | 文本，见 [文本](text) |
| `Image` / `Item` | 图像与物品图标，见 [图像](image) |

`ColorRect` 接受 `modifier` 与 `color`，`id` 必填。仓库中的调用惯例是写 `ColorRect(modifier, color) { }`——尾随 lambda 落进 `id` 槽，充当声明位置标识（见 [身份与属性](identity)）。这是刻意借用约定而非冗余代码，但它确实容易被误读，且一旦把该调用点的 lambda 提取或内联，身份就会改变。

## 已知问题
- `LateBox` 的横向布局为空实现，与"先横后纵"的全局约定冲突；同时它的垂直 aligner 不可配置。
- `AutoToPoint` 只在一侧做钳制，`AutoToRect` 的垂直判据只看单侧。
- `ShrinkBox`、`LateBox`、`MouseTip`、`DefaultBackground`、`ColorRect` 目前缺少自动化测试。
