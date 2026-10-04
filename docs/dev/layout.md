---
title: 布局
nav_order: 4
parent: Dev
---

# 布局
布局要回答的是"每个组件占多大、放在哪"。KotlinMCUI 把它拆成三层：

1. **尺寸表达**：`Measure` 表示一个长度，可能是数值，也可能是"自动"；
2. **约束声明**：`Modifier` 声明期望尺寸、最小尺寸、内边距、权重、对齐；
3. **空间分配**：`Aligner` 拿到一串 `Alignable` 与一个可用区间，把每个元素的位置写回去。

第三层是策略，前两层是数据。这样容器只需要换一个 `Aligner` 就能得到完全不同的排布，而不必修改测量逻辑。

## `Measure`
`Measure` 是包装 `Double` 的 value class，构造器私有，只能通过 `Double.px` / `Float.px` / `Int.px` 或工厂方法产生。

它需要表达"数值"以外的两种意思，于是借用了两个**保留的 NaN 位模式**作为哨兵：

| 常量 | 含义 | 位模式 |
| --- | --- | --- |
| `AUTO` | 由内容或父容器决定 | `0x7FF1000000000000` |
| `AUTO_MIN` | 固定为最小尺寸 | `0x7FF2000000000000` |

两者都是 signaling NaN，所以 `isNumber` 为 `false`。判据有两个层次：

- `isNumber`：是否为普通数值；
- `isAutoMin`：是否为 `AUTO_MIN`（比较位模式，是唯一能区分两种哨兵的方式）。

`isQNaN` 判断"quiet NaN"，即普通意义上的 NaN，两个哨兵都不是。

### 算术中的传播
| 运算 | 遇到 NaN 时 |
| --- | --- |
| `plus` / `minus` | 返回该 NaN 操作数，**左侧优先** |
| `times(Double)` / `div(Double)` | 返回 `this`，位模式原样保留 |
| `unaryMinus` | 恒等（不翻转符号） |
| `div(Measure)` | 无守卫，返回**裸 `Double`** |

左右优先导致加法**不满足交换律**：

```kotlin
Measure.AUTO + Measure.AUTO_MIN   // AUTO
Measure.AUTO_MIN + Measure.AUTO   // AUTO_MIN
```

这不是缺陷而是取舍：哨兵要能穿过内边距、外边距这类算术继续存在，才有意义；而"两种自动尺寸相加"本身没有明确定义，取左侧至少是确定的。

`div(Measure)` 返回裸 `Double` 是刻意的——它用于求比例，而比例必然是普通数值。代价是没有守卫：`AUTO / 2.px` 得到 NaN，除以零得到无穷大。

### 哨兵会被"静默降级"
位模式只在 `Measure` 自己的运算符里被精确保留。**任何经由裸 `Double` 运算的路径都会把 signaling NaN 变成普通 quiet NaN**，从而丢失哨兵身份。

已知的这类路径：

- `Iterable<Measure>.sumOf` / `minOf` / `maxOf` 实现为 `selector(it).raw` 的普通数值运算；
- `MeasureArray` 的聚合操作；
- `Rect` 上的乘法（`0 * 无穷大` 之类）。

后果是 `isAutoMin` 变假、`isNumber` 仍然为假，于是该值会被当成 `AUTO` 处理而不是 `AUTO_MIN`。凡是需要区分两种自动尺寸的地方都必须避免让值经过这些路径。

### `==` 不能区分数值语义
value class 自动生成的 `equals` 走 `Double.compare`，而 `Double.compare` 认为所有 NaN 相等：

```kotlin
Measure.AUTO == Measure.AUTO_MIN   // true
Measure.AUTO == Double.NaN.px      // true
```

因此判断自动尺寸必须用 `isAutoMin` / `bits`。同样地，把 `Measure` 当作 map 键时，所有哨兵会塌缩成同一个键。

### 数值提取
`pixelsOrElse` 是统一出口：非数值时走默认分支。在此之上有几种策略：

- `pixelsOrElse { 0.0 }`：静默回退（布局中最常用）；
- `pixelsOrElse { null }`：得到可空值，调用者自行判断；
- `pixelsOrWarn`：回退的同时打一条警告（附堆栈），用于"本该是数值却拿到了 NaN"的可疑情况；
- `requireNonNaN`：直接报错，用于不变式必须成立的位置。

`Measure.unscaled` 用 `pixelsOrElse { 0.0 }`，即**静默**把 NaN 当 0——滚动相关计算依赖这一点。

`Measure` 自带序列化器，序列化的是**位模式**而非数值，因此哨兵能完整往返。

## `Modifier`
`Modifier` 是一个只有默认实现的接口，伴生对象就是"全默认"的那一份。所有修改函数都返回 `object : Modifier by this { override val ... }`——**每个被覆写的属性产出一个包装对象**。

这是典型的不可变链：组合简单、没有别名问题、互不相关的属性与顺序无关。代价是每次修改都分配一个对象，且链的长度与修改次数成正比。

累加语义分两类：

- **累加**：`padding` 系列与 `minWidth` / `minHeight`。多次 `padding` 会相加；多次 `minWidth` 取最大值（`Measure.max` 会把哨兵也算进去）。
- **覆盖**：`width` / `height` / `weight` / `align`。后写的生效。

尺寸与最小尺寸的关系由两个派生属性定义：

```kotlin
contentMinWidth  = width.ifNan { minWidth }
contentMinHeight = height.ifNan { minHeight }
```

即"声明了宽度就用宽度，否则回退到最小宽度"。这正是 `Measure.minSize` 能被子组件撑大的机制：子组件所需尺寸进入 `minSize`，而 `size` 保持自动。

**没有最大尺寸选项**，这是明确的设计决定：最大尺寸使用频率低，且会与"父容器分配空间"的语义产生大量冲突。

`padding` 在测量中的处理方式值得注意：`Alignable` 适配器把内边距**并入外层坐标**，`minSize` 上报的是"内容最小尺寸 + 内边距"，而声明尺寸上报的是"声明尺寸 + 内边距"。也就是说布局层看到的一切都是含内边距的尺寸，内容区由适配器在读写 `rect` 时自行几何偏移。

## `Bound` 与 `Rect`
`Bound` 是**一维区间**（`low` / `high`）及其派生操作：`size`、`center`、`isEmpty`、`contains`、`expand`、`alignIn` / `alignOut`，以及按 `Align` 从位置与尺寸构造区间的工厂。

把一维区间抽出来是这套布局能保持轴无关的原因：`Aligner` 只认识 `Bound` 与 `Alignable`，同一个策略既能用在水平方向也能用在垂直方向，不需要写两遍。

`Rect` 是四边定义的矩形，**对外不可变**（接口只有 getter），由 `RectImpl` 实现；需要才能修改的地方用 `MutRect`。两者共享同一批扩展：`width` / `height` / `center` / `isEmpty` / `contains` / `overlap` / `expand` / `bounds`。

几个实现细节体现了"用视图代替复制"的取向：

- `Rect(horizontal, vertical)` 用委托属性把两个 `Bound` 拼成一个 `Rect`，不复制数值——改 `Bound` 就能看到 `Rect` 变化；
- `Rect.fixed` 返回一个写入被忽略的 `MutRect` 视图，用来把不可变矩形接到需要可变矩形的位置；
- `Rect.empty` 是一个零尺寸sentinel；`Rect.vertices` 是只读视图而不是列表；
- `Rect.toInt()` / `toFloat()` / `toDouble()` 在任一边不是数值时返回空矩形（静默）——注意是**整个矩形**一起塌成 `(0,0,0,0)`，而不是只把坏掉的那一边填 0。

`transformedMaxBound` 在变换为单位矩阵时直接返回 `this`（引用相等短路），否则把四个顶点变换后取轴对齐包围盒。它服务于"旋转/缩放后的实际占位矩形"这一类需求。

坐标约定：原点左上、Y 轴向下，`top < bottom`。

## `Alignable` 与 `Aligner`
`Alignable` 是布局层对组件的最小视图，四个量都是**提示**，策略可以随意忽略：

| 成员 | 含义 |
| --- | --- |
| `minSize` | 至少需要多少空间 |
| `size` | 期望尺寸，`AUTO` 表示由内容决定 |
| `weight` | 分配剩余空间时的权重 |
| `align` | 在自己分到的区间内如何对齐 |

两个派生函数是策略的基础：

```kotlin
autoSizeMin = size.ifNan { minSize.ifNan { 0.px } }
autoSize(default) = if (size.isAutoMin) minSize.ifNan { 0.px }
                    else size.ifNan { default }
```

**`autoSize` 里两个分支的顺序是关键的**：必须先判 `isAutoMin`，否则 `AUTO_MIN` 会被后面的 `ifNan` 吞掉，最小尺寸约束静默退化成"由父容器分配"。

注意 `size.isAutoMin` 能生效的前提是哨兵没有被裸 `Double` 运算降级（见上文）。

### 内建策略
| 策略 | 行为 |
| --- | --- |
| `equalSpacing` | 所有元素按自身尺寸排开，剩余空间**均分到每个元素之间**（含两端各半份） |
| `close(align)` | 所有元素紧贴排列，整体按 `align` 靠 `LOW` / 居中 / 靠 `HIGH`；忽略权重 |
| `simplePlace` | 每个元素**独立**占据整个区间，在自己的区间内按 `align` 摆放到声明尺寸或填满 |
| `weighted` | 声明了尺寸的元素占固定量，剩余空间按权重分给自动尺寸元素 |
| `weightedStrictByMin` | 迭代分配：按权重分剩余空间，低于自身最小尺寸的元素被钉在最小尺寸上，然后**重新分配**，直到没有元素被钉住 |
| `weightedExtend` | 每个元素至少拿到自身尺寸，剩余部分按权重分配给自动尺寸元素 |
| `equalWeightedExtend` | 与上一项类似，但剩余空间在自动尺寸元素间**等分**，不考虑权重 |

`weightedStrictByMin` 是 `Row` / `Column` 的默认策略，也是唯一一个需要迭代的策略——它解决的是"按权重分配后某些元素反而小于自身最小尺寸"的问题。迭代终止条件是没有元素再被钉住，或者剩余空间已经不足（此时全部自动元素退化为自身最小尺寸）。

> 后四个策略在主代码中都没有调用点，只被测试引用。它们对"分配不到空间"的处理是统一的：**自动尺寸元素回退到自身最小尺寸，而不是拿到零或负数**。因此当权重总和为 0、或固定尺寸元素已经用满整个区间时，结果仍然是有限且不低于下限的。
>
> 注意 `weighted` 与 `weightedStrictByMin` 的分工：前者只做一次线性分配，不保证每个元素达到最小尺寸；后者通过迭代保证这一点。需要"不小于最小尺寸"时应选后者。

### 谁用哪个策略
| 容器 | 水平 | 垂直 |
| --- | --- | --- |
| `Box` | `simplePlace` | `simplePlace` |
| `Row` | `weightedStrictByMin` | `simplePlace` |
| `Column` | `simplePlace` | `weightedStrictByMin` |
| `LateBox` | 不参与 | `simplePlace`（硬编码） |
| 文本、可滚动容器 | 视需要 | 视需要 |

`Box` 用 `simplePlace` 是"层叠"的正确语义：每个子元素都能独立使用整个区间。

`Row` 的纵向、`Column` 的横向也用 `simplePlace`，即子元素在这两个方向上各自填满或按对齐摆放——这也是"父容器总会被子组件填满"这一默认行为（正式文档里的）来源。

## `Alignment`
`Align` 是轴无关的三值枚举：`LOW` / `MID` / `HIGH`。`Alignment` 把水平与垂直两个 `Align` 打包进一个 `Int`（低位字节水平、高位字节垂直），并提供 `left()` / `top()` / `right()` / `bottom()` / `middle()` 等链式写法。默认是水平与垂直都居中。

`Modifier.align { }` 接收一个返回 `Alignment` 的 lambda，其接收者类型是 `DslPreventContext`——这个上下文只用来阻止在 align 块内部再触发聚合行为。

## 布局顺序
**全树先做横向布局，再做纵向布局。**

这不是实现细节而是语义保证：`TextAutoFold` 需要先知道可用宽度，才能折行、才能算出所需高度。如果纵向先做，文本的 `outerMinHeight` 就无法计算。

唯一的受控例外是 `LateBox`：它把自己的子节点构建推迟到纵向布局阶段，因此它的横向布局是空实现。代价是它无法再被子节点撑大。

每个容器的 `layoutHorizontal` 做两件事：先让 `Aligner` 把子节点摆进自己的区间，再递归调用子节点的 `layoutHorizontal`。因此布局是**自顶向下**的——父先决定分配给子多少空间，子再在自己分到的空间里安排自己的子。
