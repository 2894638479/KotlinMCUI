---
title: Home
layout: home
nav_order: 1
permalink: /
---
# 主页
项目处于测试阶段，任何api有可能变动。

[源码(source)](https://github.com/2894638479/KotlinMCUI/)

[默认后端实现(default backend)](https://github.com/2894638479/KotlinMCUI-backend/)

如果后端没有你所需要的版本，可以自行fork并自行做些许修改，目前一个后端大约共500行代码。
## 简介
`KotlinMCUI`是使用`Kotlin DSL`制作的ui框架。

## 功能
- 广泛二进制兼容
  - 不依赖任何`fabric/forge/neoforge`等`api`
  - 不依赖`minecraft`代码
  - 提供跨平台入口点
  - 兼容性靠后端适配提供


- `Kotlin DSL`实现的声明式、响应式布局
  - 原版`minecraft`风格
  - 整体缩放、局部缩放
  - 支持纯键盘操作
  - 文本渲染
    - 翻译
    - 颜色、字号
    - 自动换行
    - 可编辑文本
    - 下划线、阴影等样式
  - 图片渲染
    - 资源图片
    - 本地图片
    - 多种裁切策略
    - `NineSlice`
  - 焦点管理、悬停高亮
  - 无障碍朗读
  - 状态实时刷新
    - 局部变量
    - 插值动画
  - 常用基本组件
    - `Button`
    - `Slider`
    - `Container`、`Slot`、`Item`
    - `EditableText`
    - `Row`
    - `Column`
    - `LazyColumn`
    - ...

## 用途
### 如果你只是想通过ui界面提供服务
如本地文件管理、网络查询、数值计算：
`kotlinmcui`能提供一个二进制兼容层，只需要基于此开发一个`jar`就能兼容所有（甚至将来的）`minecraft`版本。


### 如果想开发`minecraft`代码相关的逻辑
仍然需要每个版本打包一个`jar`，并自行处理ui之外代码的兼容。
`kotlinmcui`能提供一个优美的DSL ui框架，减少ui代码的编写难度，并在将来迁移`minecraft`版本时无需更改ui代码。

`kotlinmcui`自身就是一个这种类型的mod。
### 如果想开发独立`java/kotlin` GUI程序/游戏
emmm...原理上也可以用，但是要自行适配后端。另外注意：这是为游戏场景设计的，每帧都会完整渲染，静态功耗会比专门的GUI框架更高。
## 实现
基于`kotlin`的`lambda`简化语法以及实验性功能的`context parameter`。由于`context parameter`目前是`kotlin`的实验性功能，所以可能并不稳定。

没有使用编译器插件，维护简单并且兼容性会更好。

语法与`Jetpack Compose`类似，但是使用立即模式绘制（更适用于游戏），因此不需要像`Compose`那样需要维护状态。你可以直接把`minecraft`原有的字段传入ui函数并保证ui实时刷新。

## 开始
[开始](https://2894638479.github.io/KotlinMCUI/start)