---
title: 上下文
nav_order: 1
parent: Dev
---

# 上下文
用户书写的 UI 代码需要一个"当前正在构建什么"的隐式环境：当前身份、当前子节点列表、当前缩放、当前主题色。KotlinMCUI 用 **Kotlin 上下文参数（context parameters）** 表达这件事，而不是自定义接收者加 `@DslMarker`，也不用编译器插件。

## 为什么不直接用一个接收者
如果所有 DSL 函数都写成 `DslContext.() -> Unit`，那么任何拿到上下文的函数都能读写全部状态，无法在类型上区分"这个函数只读缩放"与"这个函数会注册子节点"。

因此上下文被拆成一组**最小能力接口**：

| 接口 | 提供 | 典型使用者 |
| --- | --- | --- |
| `DslScaleContext` | `scale` | `scaled` 单位换算、子组件继承缩放 |
| `DslIdContext` | `identity` | 身份派生、属性作用域寻址 |
| `DslChildrenContext` | `children` | `collect` / `remove` |
| `DslDataStoreContext` | `dataStore` | `backend`、`translate` 等派生属性 |
| `DslColorContext` | `color` | `Color.mixed` 主题混色 |
| `DslPreventContext` | 阻止聚合 | `Modifier.align` 的参数 |
| `DslExecuteContext` | `dataStore` | 属性/动画初始化块、事件回调 |
| `DslTextBuilderContext` | 文本构建 | `emit` 等 |
| `DslFrameContext` | 帧信息 | 需要按帧求值的表达式 |

每个接口都带 `companion object { operator fun invoke(...) }` 工厂，返回匿名实现，因此可以凭空构造一个只含单一能力的上下文（例如 `DslIdContext(id)`）临时满足某个函数的签名要求。

用 `context(...)` 声明的函数在调用点自动取得所需能力，调用者只需要身处一个提供了这些能力的词法范围内。

## 单一可变上下文对象
`DslContext` 是 `open class`，实现上面若干能力接口。它**不是**不可变的值对象，而是一个被反复复用的可变槽位容器：

| 槽位 | 初值 | 含义 |
| --- | --- | --- |
| `_identity` | 未设置 | 当前节点身份 |
| `_children` | 未设置 | 当前可注册子节点的集合 |
| `_scale` | `Double.NaN` | 当前缩放 |
| `_color` | `Color.WHITE` | 当前主题色 |
| `_overlays` | 未设置 | 当前 Overlay 收集槽 |

进入一个作用域时保存旧值、写入新值；离开时在 `finally` 中恢复。这一保存/恢复对由 `withField` 统一实现，对外暴露成 `withIdentity` / `withChildren` / `withScale` / `withColor` / `withOverlays`。

这样做是出于性能：整棵树**每帧都会重建**，如果每进入一层容器都新建一个上下文对象，逐帧分配量会与树的深度成正比。复用同一个对象之后，进入/离开作用域只付出两次字段读写。

### 未设置的槽位必须报错
"没有当前缩放"和"缩放是 0"是完全不同的两件事。`_scale` 用 `NaN` 初始化，读取时校验：

- 未设置的身份 → `undefined identity`
- 未设置的子节点集合 → `undefined children`
- 未设置或为 NaN 的缩放 → `undefined scale`
- 未设置的 Overlay 槽 → `undefined overlays`

宗旨是让误用立刻炸掉，而不是产生一个看起来合理但错误的尺寸。缩放为 0 在 `DslScreen` 的委托构造阶段是真实存在的合法取值，所以判据是 `isNaN` 而不是"等于 0"。

## 线程约束
UI 构建状态只在创建它的那个线程上有效。`DslContext` 在构造时记录 `Thread.currentThread()`，所有槽位的读写都先经过 `checkThread()`，不匹配时报出当前线程与期望线程。

这不是可选优化，而是设计前提：上下文对象是共享可变的，一旦多个线程同时构建 UI，保存/恢复对会互相踩踏。跨线程的数据准备必须先在后台线程算好，再回到主线程渲染——见 [协程](coroutine)。

## 单位
`scale` 是"用户界面缩放"，与 Minecraft 的 GUI Scale 同源。DSL 里的尺寸有两种写法：

- `N.px`：物理像素，不随缩放变化
- `N.scaled`：等于 `px * ctx.scale`，随缩放变化

`scaled` 是对 `Double` / `Float` / `Int` 的扩展属性，需要 `DslScaleContext`。反向换算由 `Measure.unscaled` 提供。

推荐尺寸是 `9.px` 的整数倍（`9.px` 相当于缩放 1 下的一个字宽），这样在不同缩放与不同字体下都能对齐。

## `DslFunction`
```kotlin
typealias DslFunction = context(DslContext) () -> Unit
```
所有"构建一段 UI"的位置都用这个类型。它是普通函数类型，不是带接收者的 lambda，因此不能通过 `this` 访问上下文——只能通过 `context(...)` 参数声明，或者使用 `dsl` 包里的扩展属性（`identity`、`children`、`backend`、`local`、`stable`、`static` 等）。

`dsl/DslFunctions.kt` 提供常用动作：

| 函数 | 作用 |
| --- | --- |
| `newChildId(id)` | 以当前身份为前缀派生一个子身份 |
| `withId(obj) { }` | 在当前身份下追加一段并执行 |
| `withScale(scale) { }` | 局部改变缩放 |
| `withColor(color) { }` | 局部改变主题色 |
| `Iterable.forEachWithId { }` | 遍历并给每个元素一个基于元素本身的身份 |
| `collect(component)` / `remove(child)` | 手动增删子节点 |
| `translate(string, vararg args)` | 取翻译文本，缺失时回退为原文 |
| `showScreen(title, function)` | 新建并展示一个 Screen |
| `imageFile(file)` / `imageResource(location, w, h)` | 构造图像引用 |

## 颜色作用域
主题色是构建期概念。组件在自己的构造表达式里读取 `Color.WHITE.mixed` 把它**立刻求解成一个普通 `Color` 值**并保存；渲染阶段不再做任何颜色运算。所以颜色不是响应式的——`withColor` 换色只影响当帧新建的组件树，而树本来就每帧重建，因此实际表现仍然是即时生效。

`mixedWith` 是逐通道相乘（`a*b/255`），所以：

- `Color.WHITE.mixed` 等于当前主题色本身（白色是乘法单位元）；
- `withColor(Color.RED.mixed) { }` 是把红色**混入**当前主题色，得到偏红的色调；
- `withColor(Color.RED) { }` 则是**绝对设色**，与之前的作用域无关。

混色刻意放在调用点完成，框架不做隐式的事后处理。作用域在离开 `withColor` 的那一行就被恢复，不会污染后续代码。

### 作用域如何穿过延迟构建的容器
容器是延迟构建的：`Box{}` 在被书写时只创建组件，它的 body 要等到自己的 `build()` 阶段才执行，Overlay 的 body 更是排在单独的构建阶段。此时调用者的颜色作用域早已离开。

因此容器在**创建时**记下当时的主题色，在执行 body 时重新应用。这与身份、缩放的处理完全一致（组件在创建时记下身份与缩放，执行 body 时再放回去），于是

```kotlin
withColor(Color.RED) {
    Box { Button {} }   // Button 同样在红色作用域里构建
}
```

符合直觉。
