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

加载一个条目的失败被隔离在该条目内：`Exception` 与 `LinkageError`（类缺失、静态初始化块抛错等 `Error`）都只让这一个入口出局，其余照常注册——否则一个引用客户端专属类的入口就能让整套入口加载不出来。

它同时用 `Class.forName(..., initialize = false)` 只解析类而不初始化，把"是否真的使用"推迟到实例化时。

## 加载顺序
```kotlin
DslEntryLoader.init(environment)
```

首次 `init` 的动作是：加载公共入口并调用 `initialize()` → 按环境调用 `initializeClient()` 或 `initializeServer()` → 登记 GUI 入口 → 登记 Overlay 入口。

**`init` 应由后端在启动时调用一次**，重复调用只刷新登记并打一条警告：既不重跑初始化，也不会让谁的 `initializeClient()` 跑第二遍。缓存里的实例也不会被清空重建，所以 `enabledOverlays` 里的对象始终是当前这批实例。

只有初始化方法会被**调用**；GUI 与 Overlay 入口只被**登记**，等到用户打开列表或 Overlay 被启用时才执行其内容。

`DslEntryLoader.entries` 返回的是登记结果的**快照**，调用方（列表页每帧遍历它）可以安全持有与遍历，不会因为重新 `init` 而看到半截状态。

### 缓存与标志
实例按类缓存，同一个类无论通过几个接口被发现都只实例化一次。缓存项附带一组标志：

| 标志 | 含义 |
| --- | --- |
| `isCommon` / `isClient` / `isServer` | 对应的初始化方法已经执行过 |
| `isGui` / `isOverlay` | 该入口提供了对应的扩展点 |

GUI 列表页就是靠这组标志给每个入口标注徽章，并据此决定点击是否打开界面、是否显示 Overlay 开关。同一套标志也用于在开发时确认"某个入口是否真的在当前环境被加载和调用"。

## Overlay 的启用
Overlay 默认**不启用**。`enabledOverlays` 是一个有序列表：只有列在其中的 Overlay 才会被渲染，列表顺序即层叠顺序（越靠前越上层）。GUI 列表页提供开关与"置顶"操作。

根节点的 Overlay 层每帧执行：

```kotlin
enabledOverlays.forEachWithId { Box { it.overlay() } }
```

两点设计意图：

- 用 `forEachWithId` 让每个 Overlay 的身份由 Overlay 对象本身决定，而不是由书写位置决定——这样调整列表顺序不会让某个 Overlay 的状态串到别的 Overlay 上；
- 每个 Overlay 各自包一层 `Box`，使它们成为互相独立的层叠容器，一个 Overlay 内部的布局不会影响另一个。

这条路径是在根节点的 `DslScreen` 里执行的，而那个 Screen 只被建立、从不 `show()`——它是常驻的 Overlay 承载层，不是普通界面。

## 已知不一致
- 官方文档里写的 `DslEntryService.loadServices()` 在代码中不存在，实际 API 是 `DslEntryLoader.init(Environment)`；文档里给出的服务文件路径也写成 `...kotlinmcui.backend`，而真实包名是 `...kotlinmcui.entry`。
