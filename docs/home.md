---
title: Home
layout: home
nav_order: 1
permalink: /
---
# 主页
## 简介
`KotlinMCUI`是使用`Kotlin DSL`制作的ui框架。设计目标：
- 轻量
- 解耦
- 语法优美简洁
- 支持任何mc版本（甚至其它java游戏）


为了更好的跨版本/跨加载器兼容，不直接调用任何`minecraft`类/`fabric api`/`forge api`。这个模组只包含dsl实现、ui布局、测试页面等核心逻辑。因此需要为每个版本单独适配后端（工作量~500行 可fork）。

## 实现
基于`kotlin`的`lambda`简化语法以及实验性功能的`context parameter`。由于`context parameter`目前是`kotlin`的实验性功能，所以可能并不稳定。

没有使用编译器插件，维护简单并且兼容性会更好。

语法与`Jetpack Compose`类似，但是使用立即模式绘制（更适用于游戏），因此不需要像`Compose`那样需要维护状态。你可以直接把`minecraft`原有的字段传入ui函数并保证ui实时刷新。

## 开始
[开始](https://2894638479.github.io/KotlinMCUI/start.index)