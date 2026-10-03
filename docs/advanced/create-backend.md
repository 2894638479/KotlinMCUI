---
title: Create a Backend
nav_order: 4
parent: Advanced
---
# 制作一个后端

建议参考[默认后端实现(default backend)](https://github.com/2894638479/KotlinMCUI-backend/)，并可以fork其中的代码

不要求代码包名，但是需要模组id为`kotlinmcuibackend`

官方实现是怎么做的、怎么在本地构建与验证、升级游戏版本要动哪些地方，见[后端](../backend/index)一章；后端需要满足的接口契约本身见[后端接口](../dev/backend)。
## 什么时候需要制作
- 你的mod想支持的版本没有现成的后端
- 想为现有后端修复bug、改进功能、提升性能
- 想做风格化后端（ui风格）
- ~~想基于这个框架做独立程序~~

## 需要实现的事
- 将`DslBackend`提供给`dslBackendProvider`
- 把**元数据**（配置目录、游戏版本、加载器、运行环境）提供给`dslMetadataProvider`。它必须在模组初始化阶段、后端还构造不出来的时候就能用，所以这个实现不能碰窗口、渲染器、语言数据这些平台状态
- **构造`DslBackend`本身不能有副作用**：需要平台的部分放进框架会调用的时机里（创建界面、每帧渲染），不要在字段初始化器或`by`委托里就去建界面
- 调用`DslEntryLoader.init(Environment)`来发现并初始化各个入口
- 将`DslEntryPage()`注册到对应`loader`的配置页面
- 实现`DslBackend`中的函数
  - 渲染：矩形、图片、背景、按钮、物品等ui
  - 事件：剪贴板、键盘、鼠标、音效、翻译
  - 具体见源码