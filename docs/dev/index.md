---
title: Dev
nav_order: 7
---

# 开发文档
这一章记录 KotlinMCUI 的设计思路。正式文档面向使用者，只讲怎么调用；这一章面向维护者与后端实现者，讲**为什么这样设计**、有哪些不变量、以及扩展时需要遵守的约束。

阅读顺序建议：`上下文` → `身份与属性` → `组件模型` → `布局` → `渲染` → `事件`，之后按需查阅其余章节。

## 分层
```
        ┌──────────────────────────────────────────────┐
        │ 用户 DSL（context(DslContext) 函数）          │
        └──────────────────────────────────────────────┘
   dsl/ui        组件实现（Box/Row/Column/Text/…）
   dsl/decorator 行为装饰（clickable/background/…）
        ┌──────────────────────────────────────────────┐
        │ container   树与帧循环（TopComponent/DslScreen│
        │             /DslDataStore/DslChild）         │
        │ component   组件契约（事件/对齐/元数据/导航）  │
        │ prop        属性生命周期与动画                 │
        │ identity    身份链                            │
        └──────────────────────────────────────────────┘
   math           Measure/Rect/Bound/Color/Transform/…
   text          文本与字形
   image         图像引用与绘制策略
        ┌──────────────────────────────────────────────┐
        │ backend     渲染后端抽象（无 Minecraft 依赖）  │
        │ entry       加载器入口与服务发现               │
        └──────────────────────────────────────────────┘
```
核心库不含任何 Minecraft 类型。所有平台相关能力都收敛到 `backend` 包的接口，由配套后端 mod 实现；这也是同一个 jar 能同时服务 Fabric / Forge / NeoForge 的前提。

## 一帧的完整流程
后端每帧做两件事：把 `TopComponent.rect` 设为整屏尺寸，然后调用 `TopComponent.render()`。

`TopComponent` 是进程内**唯一**的根节点，所有 Screen 与 Overlay 都是它的子节点。它按 `asReversed()` 渲染子节点——这意味着**声明靠前的组件绘制在更上层**，与直觉相反，但让 `Box{}` 的层叠语义与事件命中顺序保持一致（两者都按声明顺序从前往后）。

单个 Screen 的一帧在 `DslScreen.render()` 中展开：

1. `DslDataStore.frame { … }` 推进帧号、记录帧起始时间；
2. `build()` 重新执行用户 DSL，重建整棵树并完成布局；
3. `delegate.render()` 驱动后端绘制。

`build()` 内部依次是：主树 → Overlay 层 → Tooltip/IME 层。Overlay 用循环构建，因为构建 Overlay 的过程可能产生新的 Overlay。Tooltip 层最终被**前插**到子节点列表头部，从而绘制在最上层。

帧结束时（`frame` 的 `finally`）清理未被访问的局部属性。**属性是否存活，取决于它这一帧有没有被读到**——这是整个状态管理机制的基石。

## 若干关键抉择
| 抉择 | 理由 |
| --- | --- |
| 用 Kotlin **上下文参数**而非编译器插件传递 DSL 上下文 | 不引入编译器插件依赖；每个函数只声明自己需要的能力 |
| 上下文对象**可变 + 保存/恢复**，而非每层新建 | 每帧都要遍历整棵树，避免逐层分配 |
| `scale` 以 NaN 初始化 | 误读时得到明确报错，而不是静默的 0 |
| 身份用 `DslId` 持久链表 + **lambda 的 `::class`** | 让重复书写的 `Column{}` 自动获得不同身份，无需手写 id |
| 属性按**帧戳**存活（`clearOutdated`） | 无需显式 dispose，DSL 停止书写即自动回收 |
| `Modifier` 不可变链 | 组合简单、无别名问题；代价是每个属性一层包装对象 |
| 布局固定**先横后纵** | 纵向量算可能依赖横向结果（如文本换行需要先知道可用宽度） |
| 组件用**接口委托**组合行为 | 可以在不修改类的情况下"覆写" `final` 类的成员 |
| 全局焦点/悬停/IME 状态放在单例根节点 | 这些状态本来就是进程级的，不属于某个 Screen |
| 用 toolchain 21 编译，但**链接 JDK 8 的 API 面**（`-Xjdk-release=1.8`） | 库要在旧版 Minecraft 的 Java 8 运行时上加载；只设 `jvmTarget` 不会阻止误用 JDK 9+ 的 API——那种代码编译得过，到玩家那儿才 `NoSuchMethodError` |
| 公开面按**判据**定，而不是按仓库内引用数 | 保持 public 的理由只有：① 用户要实现/继承的公开类型的契约成员；② 出现在公开签名里；③ 公开原语的对称一半或配对操作；④ 正式文档（`docs/`，不含 `docs/dev/`）教用户调用；⑤ 公开扩展点必需的原材料。对用户自己实现的类型而言"仓库内零引用"是常态，所以引用数既不能证明该收窄、也不能证明该公开。向下还有两级：`internal`（本模块，测试也算消费者）与 `private`（仅本文件）；一个声明只有本文件在用就不该停在 `internal`。 |
| 运行期依赖全交给平台，库只声明**编译期**依赖 | 消费者能拿到的元数据只有发布出去的 POM/模块元数据。把这些依赖写成运行期依赖，等于在平台已经提供它们的前提下再塞一份，可能和平台内嵌的版本撞车；只写 `compileOnly` 又没如实说明公开签名需要它们。`compileOnlyApi` 因此按"公开签名里出现过没有"来分：出现过的（slf4j、coroutines、serialization-core）进消费者的编译类路径但不进运行期元数据，没出现过的（fastutil、serialization-json）只做编译期依赖 |

一个刻意留下的例外：`PropertyLifeScope` 是公开类型但构造器是 `internal`——用户只会通过 `local`/`stable`/`static` 拿到框架建好的实例，自己构造没有意义；它的 `clear()`/`clearOutdated()` 也保持 `internal`，因为 `clear()` 会清掉跨模组共享的 static 表。

## 决策记录
有些抉择值得单独展开——被否的备选、以及不明显的后果。它们写在 `adr/` 下，是开发文档而不是站点页面：

- [预览拒绝参数不全有默认值的页面](adr/0001-preview-parameterised-pages.md)
- [核心库只声明编译期依赖](adr/0002-compile-time-dependencies.md)
- [核心库不限定版本、加载器与环境](adr/0003-version-agnostic-core.md)

## 模块地图
| 包 | 职责 |
| --- | --- |
| `context` | 上下文参数类型、`DslContext`、单位换算 |
| `identity` | `DslId` 身份链 |
| `prop` | 局部/稳定/静态属性作用域、动画、属性组合子 |
| `component` | `DslComponent` 契约、对齐适配、事件契约、焦点导航 |
| `container` | `TopComponent`、`DslScreen`、`DslDataStore`、`DslChild`、`DslScope` |
| `dsl` | `DslFunction`、DSL 辅助函数与属性快捷方式 |
| `dsl/ui` | 内置组件 |
| `dsl/decorator` | 效果装饰器与事件装饰器 |
| `modifier` | `Modifier` 链 |
| `input` | 输入类型：`Key`、`KeyModifiers`、`MouseButton`、`PreeditInfo` |
| `math` | `Measure`、`Rect`/`Bound`、`Position`、`Color` |
| `math/rect` | `Rect`/`Bound` 及 `MutRect`/`MutBound`（可写视图）、`Int`/`Float`/`Double` 专用矩形 |
| `math/align` | `Alignable`、`Aligner`、`Alignment`、`OverlayAlign` |
| `math/animate` | `Interpolatable`、`Interpolator` |
| `math/transform` | `Transform` |
| `scroll` | 滚动模型与滚动操作：`Scroller`、`AutoScroll`、`ScrollConstraint` |
| `text` | 文本构建器、文本布局、字形、可编辑字符串、翻译 |
| `image` | `ImageHolder` 与绘制策略 |
| `backend` | 后端接口、渲染原语、线程调度 |
| `logger` | 全库共用的 slf4j 日志器 |
| `config` | 配置文件的读写工具，框架自身的配置也走这里 |
| `entry` | 加载器入口接口与入口发现 |
| `widget` | 高层包装控件（`Presets` 便捷函数、`Editors` 属性编辑控件、`Colors` 颜色编辑控件）：只用公开 API 写成，与 `dsl` 一样对用户暴露 |
| `test` | 内置演示页与调试叠层 |

## 后端
核心库只定义后端要满足的契约，具体平台实现不在这个仓库里。[后端接口](backend)讲这份契约本身与它的不变量；[Fabric 后端实现](backend-fabric)讲官方后端是怎么满足它的——包括为什么要动 Mixin、为什么需要访问扩大器、以及渲染状态模型带来的约束。
