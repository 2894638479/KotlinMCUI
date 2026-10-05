---
title: 组件模型
nav_order: 3
parent: Dev
---

# 组件模型
一个组件要同时回答四类问题：**它长什么样**（尺寸与修饰）、**它怎么排布子节点**、**它怎么响应输入**、**它在无障碍与 IME 里扮演什么角色**。KotlinMCUI 把这四类问题拆成若干接口，全部有默认实现，再由一个总接口聚合起来。

## 契约分解
| 接口 | 回答的问题 | 关键成员 |
| --- | --- | --- |
| `DslComponentMetadata` | 元数据与无障碍 | `modifier`、`transform`、`instance`、`focusable`、`tooltip`、`narration`、`narratable`、`highlightable`、`preeditArea`、`selfRenderPreedit` |
| `DslComponentAlign` | 测量与对齐适配 | `rect`、`contentMinWidth/Height`、`alignableHorizontal/Vertical` |
| `DslComponentNavigator` | 焦点导航 | `viewHorizontal`、`viewVertical`、`viewSequential` |
| `DslComponentEvent` | 输入事件 | `render`、`keyDown/Up`、`mouseDown/Up/Move`、`mouseScroll*`、`charTyped`、`dropFiles`、`testHit` |
| `DslComponentGlobalEvent` | 全局状态变更 | `globalFocusChanged`、`globalHoverChanged`、`globalPreedit` |
| `DslComponent` | 之上全部 + `build` / `layoutHorizontal` / `layoutVertical` | |

`DslComponent` 同时继承 `DslIdContext` 与 `DslScaleContext`，所以任何组件天然知道自己是谁、自己的缩放是多少。

默认值的取舍值得留意：`highlightable` 的默认实现读的是 **`instance.focusable`** 而不是 `this.focusable`。原因是装饰器链——外层装饰器希望"内层声明了可获得焦点，我就能高亮"，而 `this` 只是链上的一层。同理 `testHit` 用 `instance.rect`、`contentMinWidth` 用 `instance.modifier`。整套默认实现都锚定 `instance`，这是布局、命中、高亮三者能保持一致的前提。

## 装饰器：用接口委托组合行为
框架没有"属性插槽"或"效果列表"，行为通过**替换子节点槽位里的实例**来叠加：

```kotlin
fun DslChild.background(color: Color? = null) = change {
    val c = color ?: Color.WHITE.mixed
    object : DslComponent by it { /* 覆写若干成员 */ }
}
```

`change` 不插入新节点，只是把当前组件换成它的一个包装。因此 `children.size` 不变，身份、索引、`rect` 全部沿用——装饰是"隐身"的。

### `instance` 自引用
所有层都委托到**同一个** `instance` 字段（委托到最内层实现里的那个字段）。`attachInstance()` 只在最外层调用一次，写入的就是最外层装饰器。于是链上任何一层读 `instance` 都得到最外层对象。

```
最外层装饰器  ← instance 指向这里
   └─ 内层装饰器
        └─ 原始组件（持有 instance 字段）
```

这解决了一个具体问题：外层装饰器想知道"最终这个组件能不能获得焦点"，但它看不到更内层装饰器的覆写；读 `instance.focusable` 就能拿到最终答案。

### 硬性规则
- **不要覆写 `instance`、`identity`、`scale`**。它们描述的是整条链，不是当前层。
- **要读内层尺寸时必须显式写 `it.contentMinWidth`**，不能读 `contentMinWidth`（那会经由 `instance` 回到最外层，形成自引用）。
- **装饰器每帧重建**。任何跨帧状态都必须走 `local` / `stable` / `static`，不能在装饰器对象里放普通字段。
- 需要协程作用域时，在对象体里提前取 `val coroutineScope = local.coroutineScope`。放在回调里取只在回调触发的那一帧被访问，会被当成过期项回收。
- 覆写带上下文参数的成员时，必须逐字复制 receiver 列表与泛型参数（`context(...)` 与 `<RP>`），否则不构成有效覆写。
- `instance` 只能写入一次；在组件已经挂载之后再追加装饰器会报错。

### 装饰器目录
效果类（绘制在内容下层或上层）：

| 装饰器 | 作用 | 绘制位置 |
| --- | --- | --- |
| `background` / `backgroundImage` | 纯色 / 图片底 | 内层之下 |
| `outline` / `highlightBox` | 边框 / 高亮框 | 内层之下 |
| `containerBackground` / `slotBackground` / `tooltipBackground` | 原版风格的容器、槽位、提示底纹 | 内层之下 |
| `mask` / `hoverMask` | 整体遮罩 / 悬停高亮 | 内层之上 |
| `shrink` | 上报尺寸为 0，配合外层容器决定实际大小 | 改测量 |
| `widthRate` / `heightRate` | 按比例缩放上报尺寸 | 改测量 |
| `animateHeight` / `animateWidth` | 尺寸变化走动画 | 改测量 |
| `renderScissor` | 绘制裁剪 | 裁剪 |
| `reverseChildren` | 子节点逆序绘制 | 改顺序 |
| `rotate` / `scale` / `move` / `shear` / `perspective` | 视觉变换 | 变换 |

带参数为"单位值"的变换装饰器（`rotate(0.0)`、`scale(1.0)`、`move(0.px)` 等）会直接返回自身，不产生包装对象。

事件类：

| 装饰器 | 作用 |
| --- | --- |
| `clickable` | 鼠标点击与回车激活，触发按钮音效 |
| `onHovered` / `onHoverChanged` | 悬停回调 |
| `onFocused` / `onFocusChanged` | 焦点回调 |
| `narrate` | 提供无障碍朗读文本 |
| `tooltip` | 提供提示内容 |
| `onFilesDropped` | 文件拖放 |
| `scissor` | 裁绘制并裁输入；输入上只筛按下/拖放/滚轮，移动与松开透传 |

`clickable` 的两个细节：`enabled = false` 时它**整体短路返回内层**，而不是产生一个"禁用态装饰器"；它命中后**无条件消费**事件，且不在意具体是哪个鼠标键——同一个矩形里如果叠了两个可点组件，靠后者会被挡住。

## 生命周期
每帧对每个组件按固定顺序调用：

1. `build()`：执行 body，产出子节点，然后递归 `build()` 子节点；
2. `layoutHorizontal()`：父节点在自己可用区间内摆放子节点，然后递归；
3. `layoutVertical()`；
4. `render()`。

**全树先做完横向布局，再做纵向布局**。这不是实现顺序上的偶然，而是语义约定：纵向测量可能依赖横向结果，最典型的是文本换行——必须先知道可用宽度，才能算出会折成几行、进而算出所需高度。个别组件（如 `LateBox`）会刻意打破这条约定见长，属于受控例外。

### 事件路由
`DslScope` 是"容器 + 事件路由器"的默认实现。它的策略是：

| 事件 | 策略 |
| --- | --- |
| `keyDown` / `keyUp` / `mouseDown` / `mouseUp` / `charTyped` / `dropFiles` | **正序**遍历子节点，第一个返回 `true` 的胜出并停止传播 |
| `mouseMove` | 遍历全部子节点，无"消费"概念 |
| `mouseScrollVertical` / `mouseScrollHorizontal` | **余量递减**：把未消费的量传给下一个子节点，减到 0 立刻返回 |
| `testHit` | 正序找到第一个满足谓词的子节点，否则回退到自身 |

滚动事件返回余量而不是布尔值，是为了让嵌套滚动容器可以自然协作：内层滚到底之后余量外溢给外层。

### 渲染顺序与命中顺序的一致
`render()` 用 `children.asReversed()` 遍历，而 `testHit` 与事件用正序遍历。两条看似矛盾的规则合起来给出一个不变式：

> **声明靠前 = 绘制在更上层 = 命中优先。**

`asReversed()` 是因为绘制是按顺序覆盖的，先画的在下层；命中从前往后探测，先声明的先被命中，正好对应"最上层优先"。用户不需要记住这两套遍历方向，只需要记住"写在前面就在上面"。

### `testHit` 的谓词语义
`testHit(mouse) { it.takeIf { ... } }` 里的 `get` 是**可失败的谓词**：返回 `null` 表示"这个组件不满足条件"，探测会继续找兄弟节点；返回非 `null` 则选中。因此它的语义是"最上层满足条件的组件"，可以一次查询焦点组件、悬停组件、可朗读组件等。

无坐标重载 `testHit { }` 用于按身份定位组件（例如找当前焦点组件），不需要鼠标位置。

裁剪与变换装饰器会各自覆写 `testHit`，使命中测试走与绘制相同的裁剪矩形/逆变换矩阵——保证"看得见就点得到"。

## 全局状态
焦点、悬停、朗读、IME 预编辑状态不放在组件上，而放在单例根节点上，因为它们是**进程级**的：同一时刻整个游戏只有一个焦点组件，切换 Screen 不应让焦点状态残留。用户可以用公开的 `TopComponent.setFocus(identity)`（`source` 默认 `FocusSource.CODE`，`null` 表示清除）主动设焦点，鼠标与键盘导航走的是同一个函数；它**不**筛选 `focusable`，所以"根节点记录了焦点"和"组件报告 `isFocused`"是两件事。

全局事件从根节点向下派发，顺序是**后序**的（先子后父）：子组件先知道自己失去了焦点，父容器最后才看到变化，便于父容器在子组件已更新后再做整体反应。

`preedit` 的派发规则特殊：文本非空时**只**发给当前焦点组件（避免所有输入框都显示同一段组合文本）；文本为空时**广播**给全部组件，作为"组合结束"的信号。

## 编写一个新组件
1. 用 `Spacer(modifier, id).change { object : DslComponent by it { ... } }` 起手，只覆写需要的成员（`Spacer` 内部就是 `DslComponentImpl`，但它是 `internal`，用户侧只能走这个工厂）；
2. 需要一个容器就用 `Scope(modifier, id, 横向 aligner, 纵向 aligner, function)`，再 `change { object : DslScope by it as DslScope { ... } }`；
3. 想暴露自定义尺寸，覆写 `contentMinWidth` / `contentMinHeight`；需要让子节点参与测量，遍历 `instance.children`，用 `outerMinWidth` / `outerMinHeight` 自行求和或取最大值；
4. 需要在同一个组件上继续叠加行为，返回 `DslChild`（而不是 `Unit`），这样调用者能链式调用装饰器；
5. 必须在构造期读取的东西（尺寸、颜色、主题）直接读；必须每帧保持的东西（状态、协程）走属性作用域。

如果组件需要在布局完成后才知道自己要构建什么（典型是自适应宽度的延后布局），可以覆写 `layoutVertical()`，在那里执行 `buildThis` 并自行调用 aligner——`LateBox` 就是这么做的，代价是它无法再被子节点撑大。
