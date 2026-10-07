---
title: 入口
nav_order: 10
parent: Dev
---

# 入口
核心库需要一种方式发现"谁提供了后端""谁想往 GUI 列表里加一个界面""谁想常驻一层 Overlay"，而它自己又必须能在客户端、服务端、以及不同加载器上加载。入口机制就是回答这个问题的。

## 五个入口接口
所有入口都实现 `DslEntryService`，提供三项身份信息：`name`（显示名）、`id`（稳定标识）、`icon`（图标引用）。

在此之上按"运行环境"和"提供什么"分成两组：

| 接口 | 组 | 方法 |
| --- | --- | --- |
| `DslEntryCommon` | 运行环境 | `initialize()` |
| `DslEntryClient` | 运行环境 | `initializeClient()` |
| `DslEntryServer` | 运行环境 | `initializeServer()` |
| `DslEntryGui` | 扩展点 | `content()` |
| `DslEntryOverlay` | 扩展点 | `overlay()` |

一个类可以同时实现多个接口，因此"既在客户端初始化、又提供一个界面"是常见组合。

`content()` 与 `overlay()` 都是 `context(DslContext)` 函数——它们是 DSL 片段，不是普通回调。Overlay 的内容会被包进一个 `Box` 再渲染。

### 为什么区分客户端与服务端
加载器会同时加载两侧的类。如果一个入口类引用了只在客户端存在的类型（渲染器、客户端 Screen 之类），在专用服务端加载它就会在**类加载阶段**直接抛错，连"这个入口不适用于服务端"的判断机会都没有。

把客户端专属逻辑放进实现 `DslEntryClient` 的类里，服务端就不会去碰它。

但这套切分**只覆盖初始化方法**：`DslEntryGui` 与 `DslEntryOverlay` 在任何环境都会被加载登记，因为它们是扩展点而不是环境相关的方法。所以一个同时实现 `DslEntryClient` 与 `DslEntryGui` 的类，在服务端**仍会被加载**，只是 `initializeClient()` 不会被调用。需要严格隔离时应当拆成两个类。

## 注册方式
在 `META-INF/services/` 下按接口全名建文件，每行写一个实现类的全限定名。例如：

```
META-INF/services/io.github.u2894638479.kotlinmcui.entry.DslEntryGui
```

### 为什么不用 `java.util.ServiceLoader`
`ServiceLoader` 要求实现类有可访问的无参构造器。Kotlin 的 `object` 声明没有公开构造器，于是最自然的写法（`object MyEntry : DslEntryGui`）会直接不可用。

自建加载器两种写法都能注册：无参构造器**可访问**时用它构造，否则改用 `INSTANCE` 静态字段——而后者才是 Kotlin `object` 的常规路径，因此不把它当成"回退"来告警。构造器存在但**抛异常**时也不会被 `INSTANCE` 悄悄顶替：那会把失败藏在一个没人要的第二个实例后面，直接跳过这个入口并带上异常打日志才是对的。

加载每个条目时会用 `Class.cast` 按**它被列在的那个接口**做一次真正的检查，不匹配就跳过并记日志（发现标志位也只在被接受之后才设置）。这一步不能省，也不能写成 `as T`：实例缓存只按类索引（同一个类经由几个接口被发现也只实例化一次），而 `as T` 对非 reified 的类型参数会被擦除成它的**上界**（编译器会给 `Unchecked cast` 警告），于是列错接口的实例会混进结果列表，真正按接口类型的检查被推迟到调用方的循环里——那里没有 `catch`，`ClassCastException` 会直接冲出整个启动流程。

加载一个条目的失败被隔离在该条目内：`Exception` 与 `LinkageError`（类缺失、静态初始化块抛错等 `Error`）都只让这一个入口出局，其余照常注册——否则一个引用客户端专属类的入口就能让整套入口加载不出来。出局的条目**不会消失**：它变成一条 `brokenEntries` 记录（见[加载不出来的条目](#加载不出来的条目)）。

它同时用 `Class.forName(..., initialize = false)` 只解析类而不初始化，把"是否真的使用"推迟到实例化时。

## 加载顺序
```kotlin
DslRuntime.install { Metadata }   // 内部调用 DslEntryLoader.discover(environment)
```

首次 `discover` 的动作是：加载公共入口并调用 `initialize()` → 按环境调用 `initializeClient()` 或 `initializeServer()` → 登记 GUI 入口 → 登记 Overlay 入口。

**`discover` 不由后端直接调用**：它由 `DslRuntime.install` 在启动时调用一次，`install` 自己保证单次（重复调用会直接报错，见[后端](backend)）。缓存里的实例不会被清空重建，所以 `enabledOverlays` 里的对象始终是当前这批实例。

只有初始化方法会被**调用**；GUI 与 Overlay 入口只被**登记**，等到用户打开列表或 Overlay 被启用时才执行其内容。

`DslEntryLoader.entries` 返回的是登记结果的**快照**，调用方（列表页每帧遍历它）可以安全持有与遍历，不会因为重新 `discover` 而看到半截状态。

### 加载不出来的条目
服务文件里列了、但用不了的条目不会静默消失。三种情形——类找不到、没实现它被列在的那个接口、构造不出来（没有可用构造器 / 没有 `INSTANCE` / 构造器或静态初始化块抛错）——都会在 `brokenEntries` 里留下一条 `Failure`：

```kotlin
class Failure(val className: String, val hookName: String, val reason: String)
```

`reason` 是给页面看的一行话（`does not implement DslEntryGui`、`class not found`、`could not be initialised (missing dependency?)`、`constructor failed: RuntimeException` 之类），完整堆栈仍然在日志里。记录按"类名 + 接口名"去重：同一个类被列错在两个文件里就是两条记录，因为需要修的正是那个文件。`brokenEntries` 与 `entries` 一样是快照，并在每次 `discover` 开始时清空。

配置页把这些记录**追加在列表末尾**，每条一行矮行：没有图标、没有开关、也不受"显示代码入口"开关影响——一个加载失败的条目正是最该被看见的东西。它比正常入口矮，因为正常入口的高度由那个 40 scaled 的图标撑着，而它只有一个自动定高的文本行加 2 scaled 内边距。

这一块解决了一个真实痛点：Overlay 的启用状态是按 **id** 存在 `overlays.json` 里的，如果那个 Overlay 的入口坏了，以前会静默什么都不画；现在能在列表里看到"你启用的那个入口坏了，原因是什么"。

### 缓存与标志
实例按类缓存，同一个类无论通过几个接口被发现都只实例化一次。缓存项附带一组标志：

| 标志 | 含义 |
| --- | --- |
| `isCommon` / `isClient` / `isServer` | 对应的初始化方法已经执行过 |
| `isGui` / `isOverlay` | 该入口提供了对应的扩展点 |

GUI 列表页就是靠这组标志给每个入口标注徽章，并据此决定点击是否打开界面、是否显示 Overlay 开关。同一套标志也用于在开发时确认"某个入口是否真的在当前环境被加载和调用"。

## Overlay 的启用
Overlay 默认**不启用**。启用状态是一个**有序 id 列表**：只有列在其中的 Overlay 才会被渲染，列表顺序即层叠顺序（越靠前越上层）。GUI 列表页的开关与"置顶"分别调用 `setOverlayEnabled` / `bringOverlayToTop`，两者都会立刻写回配置。

配置存放在 `<configDir>/kotlinmcui/overlays.json`，内容就是 `DslEntryService.id` 的有序数组（模型见 `OverlayConfig`），由[配置工具](config)读写。读取发生在 `DslRuntime.install` 期间——那时元数据（配置目录）已经就绪，而后端本体还不需要被构造。三点设计意图：

- 用 **id** 而不是实例或列表下标做标识：下标会随登记顺序变，实例每次启动都会重建，只有 id 在两次启动之间稳定；
- **未安装模组的 id 保留在文件里**：临时卸掉一个模组不会丢掉它的开关状态，装回来仍然按原来的顺序渲染；
- 两个 Overlay 共用同一个 id 时无法区分，所以登记时对重复 id 打一条警告（框架自己的演示入口就曾共用 `TestEntry` 的 id，为此拆成了各自的 id）。

根节点的 Overlay 层每帧执行：

```kotlin
enabledOverlays.forEachWithId { Box { it.overlay() } }
```

`enabledOverlays` 是"当前已登记的 Overlay 按启用顺序解析"的结果快照，因此文件里存在但当前没安装的 id 不会出现在这里。两点设计意图：

- 用 `forEachWithId` 让每个 Overlay 的身份由 Overlay 对象本身决定，而不是由书写位置决定——这样调整列表顺序不会让某个 Overlay 的状态串到别的 Overlay 上；
- 每个 Overlay 各自包一层 `Box`，使它们成为互相独立的层叠容器，一个 Overlay 内部的布局不会影响另一个。

这条路径是在根节点的 `DslScreen` 里执行的，而那个 Screen 只被建立、从不 `show()`——它是常驻的 Overlay 承载层，不是普通界面。
