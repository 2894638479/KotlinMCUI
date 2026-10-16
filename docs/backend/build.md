---
title: Build & Run
nav_order: 2
parent: Backend
---

# 构建与运行

## 两个仓库要放在一起
后端的构建脚本会优先使用**本地构建出来的核心库 jar**，找不到才回退到远程仓库：

- 找得到 `../kotlinmcui/build/libs/kotlinmcui-<版本>.jar`（和对应的 `-sources.jar`）时用它，源码包只参与编译；
- 找不到时改为从 JitPack 拉 `com.github.2894638479:KotlinMCUI:v<版本>`，而那个版本必须已经发布过。

所以本地开发时两个仓库必须是**同级目录**，且名字分别为 `kotlinmcui` 与 `kotlinmcui-backend`：

```
<工作目录>/
├── kotlinmcui/           核心库
└── kotlinmcui-backend/   后端
```

后端 `gradle.properties` 里的 `kotlinmcui_version` 必须与核心库 `gradle.properties` 里的 `mod_version` **完全一致**，否则本地 jar 找不到、构建会退化成远程依赖（可能解析到已发布的旧版本）。因此修改核心库接口后，正确顺序是：

1. 在核心库目录执行 `./gradlew build`；
2. 再在后端目录构建。

## 产物
两个仓库都用同一套命名规则：

| 仓库 | 产物 |
| --- | --- |
| 核心库 | `build/libs/kotlinmcui-<mod_version>.jar` |
| 后端 | `build/libs/KotlinMCUI-backend-<mod_version>+<加载器>+<Minecraft 版本>.jar` |

各自还有一个 `-sources.jar`。

## 启动游戏
```
./gradlew runClient
```
运行目录是仓库下的 `run/`（已加入忽略列表）。第一次运行需要联网下载 Minecraft、映射与加载器；缓存齐全之后可以加 `--offline`（Loom 的缓存按游戏版本分，换版本后第一次仍要联网）。

`run/mods` 里**不需要**手动放 jar：核心库作为依赖已经在开发运行时的类路径上。

构建用工具链固定在游戏要求的 Java 版本（26.2 是 25），本机没有时需要 Gradle 能自行解析工具链。

## 自动验证
窗口里没人操作也能验证后端是否还能用：
```
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestColorPage -Dkotlinmcui.preview.capture=true" ./gradlew runClient
```
它会在游戏出现界面之后自动打开页面、**按帧**等它渲染完，再写一张截图进 `run/screenshots/` 并退出。退出码为 0 且截图内容正常，就说明"模组能加载、界面能画、事件循环没抛异常"。这个检查需要真实的显示与 GPU，无法在无头环境里运行。

**预览不需要运行配置**：属性直接给游戏 JVM 即可，`runClient` 本身就能带上它（`JAVA_TOOL_OPTIONS` 由 JVM 自己读取，而 Loom fork 出的游戏 JVM 会继承它）：

```bash
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestTransformPage" ./gradlew runClient
```

```powershell
$env:JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestTransformPage"; .\gradlew runClient
```

页面用**引用**指定，不需要注册任何东西；返回值类型不限。页面只能长成两种样子：只要 DSL 上下文，或者额外参数**全部有默认值**——预览没有实参可传，只能按零值调用它们。只要有一个额外参数没有默认值，解析就会失败，日志会点名是哪个参数，并列出试过的引用。这就是**混合签名**被拒绝的场合：额外参数里既有必填又有带默认值的写法，没法靠零值调用补全，也没有实参可传。

| 形式 | 例子 |
| --- | --- |
| IDE 里复制的顶层函数引用（JVM 类名 = 文件名 + `Kt`，所以文件要与函数同名） | `io.github.u2894638479.kotlinmcui.test.TestColorPage` |
| 显式的类与方法：静态函数，或 object / companion / 可用无参构造器建出来的类的成员 | `io.github.u2894638479.kotlinmcui.test.TestTransformPageKt#TestTransformPage`、`io.github.u2894638479.kotlinmcui.test.TestEntry$TestPage#content` |

**预览指定的是一个函数**：只给类名不算引用，也不会去猜哪个函数画页面（那样只会报找不到）。

不设这个属性时什么都不会打开（就是普通客户端）。**只打开页面、不拍照也不退出**是默认行为，适合自己看；加了 `capture` 才变成给机器用的模式——这时**解析不出来的引用、构建或渲染时抛异常的页面、以及没写出截图，都会以非零退出码结束**（日志会列出尝试过哪些名字）。

**透视是唯一会用到位姿矩阵第三行的路径**（`test.TestTransformPage`），改动变换相关代码后应当跑一次；`test.TestPolygonPage` 是唯一走三角形列表这个原语的页面，改动顶点提交路径后应当跑一次它。

预览**不替页面补背景**：页面自己没挂 `defaultBackground()` 时，截图里控件背后就是游戏画面。`test.TestColorPage` 等演示页都自己挂了。

引用解析不出来、或参数形状不合格时，预览把待测页面换成**错误页**（写着引用与原因），并保留错误日志与非零退出码；预览会话下截的就是这一页，所以那张图本身就是失败报告。不加 `capture` 时游戏不退出，可以留在页面里读原因。

首次在一个游戏目录里运行时，原版会先显示**无障碍引导屏**并朗读「Press enter to enable the narrator」；预览直接换屏幕、不去应答它，所以该目录每次启动都会再来一遍。在 `run/options.txt` 里写 `onboardAccessibility:false` 即可关掉。

页面本身是**框架里公开的顶层 `context(DslContext)` 函数**，时序与判定也都在框架里——框架的预览会话负责解析引用、等首帧、按帧等待、判定、截图与退出码，后端只提供 `capture`（把当前帧写成文件）与 `quit`（结束游戏）两个平台动作。换一个后端时这两处不必重写。

七条分支都用这一套命令验收（清单见[概览](overview)）。预览机制本身在**核心库**里，后端只提供 `capture` 与 `quit` 两个平台动作，所以换分支不需要重写任何验证代码。图形后端方面，`26.x` 上除 OpenGL 外**建议再用 Vulkan 跑一遍同一套命令**——Vulkan 对绘制路径的校验更严格，OpenGL 能跑通不代表它能（原因见[开发文档](../dev/backend-fabric)）：

```
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestColorPage" ./gradlew runClient --args="--graphicsBackend vulkan"      # PowerShell 同样写法
```

判定标准有两条：截图内容正常（界面确实画出来了），以及日志里有 `input callbacks registered`（输入回调装上了；缺这行时游戏照常启动，但框架收不到任何输入）。

## 手动检查清单
需要人眼确认时，按这个顺序看：

1. 模组列表里同时有 `kotlinmcui` 与 `kotlinmcuibackend`；
2. 配置页能打开，且列出了注册的 GUI 入口；
3. 任意入口点进去，界面可交互（按钮有悬停高亮、滑条可拖动、文本可输入）；
4. 日志里没有 `Exception` 与 mixin 注入失败。
