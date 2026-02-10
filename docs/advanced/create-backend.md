---
title: Create a Backend
nav_order: 4
parent: Advanced
---
# 制作一个后端

建议参考[默认后端实现(default backend)](https://github.com/2894638479/KotlinMCUI-backend/)，并可以fork其中的代码

不要求代码包名，但是需要模组id为`kotlinmcuibackend`
## 什么时候需要制作
- 你的mod想支持的版本没有现成的后端
- 想为现有后端修复bug、改进功能、提升性能
- 想做风格化后端（ui风格）
- ~~想基于这个框架做独立程序~~

## 需要实现的事
- 将`DslBackend`提供给`dslBackendProvider`
- 调用`DslEntryService.loadServices()`并执行它们的`initialize()`
- 将`DslEntryPage()`注册到对应`loader`的配置页面
- 实现`DslBackend`中的函数
  - 渲染：矩形、图片、背景、按钮、物品等ui
  - 事件：剪贴板、键盘、鼠标、音效、翻译
  - 具体见源码