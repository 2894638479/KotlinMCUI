---
title: Overview
nav_order: 1
parent: Backend
---

# 概览

## 它是什么
核心库定义了后端必须提供的能力：绘制矩形/图片/物品、字体度量与字形、裁剪与变换、剪贴板、键盘鼠标状态、翻译、声音、本地图片加载。官方后端 `KotlinMCUI-backend` 把这些能力落到 Minecraft + Fabric 上，并且**没有任何游戏逻辑**——它只是适配层。

它由四部分组成：

- 绘制适配：把核心的矩形/UV/顶点请求翻译成游戏的 GUI 渲染状态；
- 输入适配：注册 GLFW 回调，把鼠标键盘事件转成核心的事件对象；
- 环境适配：配置目录、游戏目录、游戏版本、运行环境；
- 入口加载：初始化时发现并加载所有 `DslEntryXXX` 服务。

## 版本号怎么读
后端版本号由四段用 `+` 连接，空段会被跳过：

```
<后端版本>+<加载器>[+<加载器后缀>]+<Minecraft 版本>
```

例如 `1.0.0-alpha.19+fabric+26.2` 表示：后端 `1.0.0-alpha.19`、Fabric、Minecraft `26.2`。同一个 Minecraft 版本下不同加载器是不同分支，同一加载器的不同 Minecraft 版本也是不同分支。

后端版本与它依赖的核心库版本**无关**：核心库版本写在 `gradle.properties` 的 `kotlinmcui_version` 里，两者各自发布。

**后端与核心库是分开发布的，必须成对安装。** 后端的元数据把核心库写成精确版本，装错会在加载时直接报依赖不满足，而不是运行时出错。核心库那边只能把后端写成 `recommends`：专用服务端上没有后端，硬依赖会让服务端拒绝加载核心库。

## 支持情况
当前只维护 `fabric-26.2` 这一个分支：Minecraft `26.2`、Fabric、Java 25。其它 Minecraft 版本的分支不再更新；需要时按[移植](porting)的清单另开分支。

## 依赖
| 依赖 | 说明 |
| --- | --- |
| Fabric Loader | 0.19.5 起 |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 起 |
| KotlinMCUI（核心库） | 由后端的 `kotlinmcui_version` 精确指定，与后端自身的版本号无关 |
| kotlinx-serialization-json | 1.11.0；核心库用它读写配置文件。Fabric 上由 Fabric Language Kotlin 提供（其打包版本已与核心一致）；缺少该库的加载器需要后端把它打进 jar |
| Mod Menu | 可选；装了之后后端的配置页会出现在 Mod Menu 里 |
| Java | 25 |

客户端专用（`environment: client`）：它注册输入回调、注册渲染管线，这些在专用服务端都不存在。

## 配置页
后端会把所有注册进来的入口列在自己的配置页里，装了 Mod Menu 时可以从 Mod Menu 进入。列表里每一项都标出了它属于哪类入口：

- GUI 入口点击后打开对应的 `Screen`；
- Overlay 入口不会打开界面，而是在列表里提供一个开关，只有开启的 Overlay 才会叠加渲染在游戏界面上。
