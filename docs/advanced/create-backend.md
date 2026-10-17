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
- 在模组初始化时调用`DslRuntime.install { 你的元数据 }`：元数据（配置目录、游戏版本、加载器、运行环境）必须在那一刻就可读，所以它的实现只能读加载器/进程信息，不能碰窗口、渲染器、语言数据
- 平台可以创建界面之后调用一次`DslRuntime.start { 你的平台 }`（Fabric 上就是`Minecraft.<init>`返回时）；**先到先得**，第二次返回`false`而不抛错——它只意味着这次没生效（会记一条日志），你的后端照常建自己的根、照常服务。从此`dslPlatform`/`dslMetadata`可用
- 后端不再是一个"聚合对象"，而是分别实现下面几个接口：
  - `DslPlatform`：`metadata`、`utils`、`dispatcher`
  - `DslPlatformUtils`：翻译、退出、加载图片、后台调度
  - `DslRootUtils`：剪贴板、旁白、音效、跳转链接、截图、会话调度
  - `DslBackendRenderer`、`DslBackendInput`、`DslRootScreenFactory`
  - 一个对象可以同时实现多个（例如 `DslPlatformUtils` 与 `DslRootUtils` 用同一个实现）
- **根由后端自己建**：`DslRoot(input, renderer, screenFactory, rootUtils)`。框架不发根、也不替你持有根
- **根不负责渲染**：每帧写`root.rect`，然后在`context(renderer, 参数, root.input.mouse)`里调用`root.component.render()`
- **预览要显式起**：`DslPreview.start(root)`。平台发布不会再顺带触发它——预览画进哪个根，就由谁带着自己的根调用
- **根的公开入口只有两个**：`root.setFocus(id)`（由代码设焦点，`null` 清除）与 `root.close()`（幂等；之后挂载、渲染、设焦点全部响亮失败）。拆除顺序是**先 `close()`，再停你自己的帧循环与执行器**
- 将`DslEntryPage()`注册到对应`loader`的配置页面
- 具体接口见源码与[后端接口](../dev/backend)
