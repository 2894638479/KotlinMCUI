---
title: Overview
nav_order: 1
parent: Backend
---

# 概览

## 它是什么
核心库定义了后端必须提供的能力：绘制矩形/图片/物品、字体度量与字形、裁剪与变换、剪贴板、键盘鼠标状态、翻译、声音、本地图片加载。官方后端 `KotlinMCUI-backend` 把这些能力落到具体的加载器与 Minecraft 版本上（Fabric、Forge、NeoForge 各有分支），并且**没有任何游戏逻辑**——它只是适配层。

它由四部分组成：

- 绘制适配：把核心的矩形/UV/顶点请求翻译成游戏的 GUI 渲染状态；
- 输入适配：接管平台的鼠标键盘事件，把它们转成核心的事件对象（`1.20.1`/`1.21.1` 上是 GLFW 回调，`26.3` 起游戏改用 SDL，改为注入游戏自己的输入处理器）；
- 环境适配：配置目录、游戏目录、游戏版本、运行环境；
- 入口加载：初始化时发现并加载所有 `DslEntryXXX` 服务。

## 版本号怎么读
后端版本号由四段用 `+` 连接，空段会被跳过：

```
<后端版本>+<加载器>[+<加载器后缀>]+<Minecraft 版本>
```

例如 `1.0.0-alpha.19+fabric+26.3` 表示：后端 `1.0.0-alpha.19`、Fabric、Minecraft `26.3`。同一个 Minecraft 版本下不同加载器是不同分支，同一加载器的不同 Minecraft 版本也是不同分支。

后端版本与它依赖的核心库版本**无关**：核心库版本写在 `gradle.properties` 的 `kotlinmcui_version` 里，两者各自发布。

**后端与核心库是分开发布的，必须成对安装。** 后端的元数据把核心库写成精确版本，装错会在加载时直接报依赖不满足，而不是运行时出错。反方向同样是硬依赖，而且**两侧都一样**：核心库需要 Kotlin 运行时，而运行时用哪一家（Fabric Language Kotlin / Kotlin for Forge / KotlinLangForge）由后端挑选提供；更重要的是框架的每一行代码都要经过后端能力，服务端也不例外——只装核心库不装后端，服务端连一行代码都跑不起来。所以核心库把后端的 mod id 写成 `depends` / `mandatory = true` / `type = "required"`。

官方目前只有客户端实现。服务端的实现有两条路可选：只提供入口发现（同一份 jar 也允许被客户端加载），或连同窗口管理与渲染一起提供——后者能让一份 jar 跨多个 Minecraft 版本、只按加载器分。

## 支持情况
一条分支只对应**一个加载器 + 一个 Minecraft 版本**。当前维护七条分支，全部按同一套流程验收（构建通过 + 游戏内预览出图）：

| 分支 | 加载器 | Minecraft | Java | 构建工具链 |
| --- | --- | --- | --- | --- |
| `fabric-1.20.1` | Fabric | 1.20.1 | 17 | Fabric Loom 1.13-SNAPSHOT（需要 remap 的旧 DSL） |
| `fabric-1.21.1` | Fabric | 1.21.1 | 21 | 同上 |
| `fabric-26.3` | Fabric | 26.3 | 25 | Fabric Loom 1.18.2 |
| `forge+kff-1.20.1` | Forge | 1.20.1 | 17 | ForgeGradle 6.0.54 + **Gradle 8.14.5 + JDK 17**（ForgeGradle 拒绝 Gradle 9） |
| `forge+klf-1.20.1` | Forge | 1.20.1 | 17 | 同上，Kotlin 提供者换成 KotlinLangForge |
| `neoforge+kff-1.21.1` | NeoForge | 1.21.1 | 21 | ModDevGradle 2.0.148 + Gradle 9.8.0 |
| `neoforge-26.3` | NeoForge | 26.3 | 25 | 同上，Kotlin 提供者换成 KotlinLangForge |

`fabric-26.2` 是 `26.3` 的前身（也是当前已发布的那一条），保留但不再跟进新版本。需要新的组合时按[移植](porting)的清单另开分支。

## 依赖（以 Fabric 分支为例）
| 依赖 | 说明 |
| --- | --- |
| Fabric Loader | 0.19.5 起 |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 起 |
| KotlinMCUI（核心库） | 由后端的 `kotlinmcui_version` 精确指定，与后端自身的版本号无关 |
| kotlinx-serialization-json | 1.11.0；核心库用它读写配置文件。核心库把它（以及 slf4j、fastutil、coroutines、serialization-core）都声明成**编译期**依赖：运行期由平台提供——Minecraft 自带 slf4j 与 fastutil，Fabric Language Kotlin 内嵌 coroutines 与 serialization。缺少某个库的加载器需要后端把它打进 jar |
| Fabric API | **必装**：Fabric 上加载模组资源（核心库的语言文件就在其中）的是它的资源加载器。缺了它，打包后的客户端里所有翻译都会退化成键名——开发环境看不出来，因为那里由 Loom 代劳 |
| Mod Menu | 可选；装了之后后端的配置页（入口点列表）能从 Mod Menu 进入。Fabric 上它没有别的 UI 入口，不装时你自己的页面仍可用 `showScreen` 打开 |
| Java | 运行游戏用的版本见上面的支持情况表；**构建用的 JDK 由构建工具链决定**，Forge 分支必须是 JDK 17 |

Forge 与 NeoForge 分支把语言提供者换成 Kotlin for Forge / KotlinLangForge，依赖关系其余相同——核心库自己的元数据里不出现任何提供者的名字，用哪一家由后端挑选。

**客户端专用**（各加载器写法不同：Fabric 是 `environment: client`）：它注册输入回调、注册渲染管线，这些在专用服务端都不存在。

## 配置页
后端会把所有注册进来的入口列在自己的配置页里，装了 Mod Menu 时可以从 Mod Menu 进入。列表里每一项都标出了它属于哪类入口：

- GUI 入口点击后打开对应的 `Screen`；
- Overlay 入口不会打开界面，而是在列表里提供一个开关，只有开启的 Overlay 才会叠加渲染在游戏界面上。
