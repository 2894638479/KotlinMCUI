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

后端 `gradle.properties` 里的 `kotlinmcui_version` 必须与核心库 `gradle.properties` 里的 `mod_version` **完全一致**。不一致时本地 jar 找不到，构建会悄悄退化成远程依赖——症状是"改了核心库但后端行为没变"，或者本地根本没网时解析失败。

因此修改核心库接口后，正确顺序是：

1. 在核心库目录执行 `./gradlew build`（或 `publishToMavenLocal`）；
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
运行目录是仓库下的 `run/`（已加入忽略列表）。第一次运行需要联网下载 Minecraft、映射与加载器；缓存齐全之后可以加 `--offline`。

`run/mods` 里**不需要**手动放 jar：核心库作为依赖已经在开发运行时的类路径上。

## 自动验证
窗口里没人操作也能验证后端是否还能用：
```
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestColorPage -Dkotlinmcui.preview.capture=true" ./gradlew runClient
```
它会在游戏出现界面之后自动打开页面、**按帧**等它渲染完；加 `-Dkotlinmcui.preview.capture=true` 时再写一张截图进 `run/screenshots/` 并退出。退出码为 0 且截图内容正常，就说明"模组能加载、界面能画、事件循环没抛异常"。

**预览不需要运行配置**：属性直接给游戏 JVM 即可，`runClient` 本身就能带上它（`JAVA_TOOL_OPTIONS` 由 JVM 自己读取，而 Loom fork 出的游戏 JVM 会继承它）：

```bash
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestTransformPage" ./gradlew runClient
```

```powershell
$env:JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestTransformPage"; .\gradlew runClient
```

页面用**引用**指定，不需要注册任何东西：

| 形式 | 例子 |
| --- | --- |
| IDE 里复制的顶层函数引用（JVM 类名 = 文件名 + `Kt`） | `io.github.u2894638479.kotlinmcui.test.TestColorPage` |
| 显式的类与方法 | `io.github.u2894638479.kotlinmcui.test.TestTransformPageKt#TestTransformPage` |
| 类名（入口，用它的 `content()` 画） | `io.github.u2894638479.kotlinmcui.test.TestEntry$TestPage` |

不设这个属性时什么都不会打开（就是普通客户端）。**只打开页面、不拍照也不退出**是默认行为，适合自己看；再追加 `-Dkotlinmcui.preview.capture=true` 才变成给机器用的模式——这时**解析不出来的引用、构建或渲染时抛异常的页面、以及没写出截图，都会以非零退出码结束**（日志会列出尝试过哪些名字）。

**透视是唯一会用到位姿矩阵第三行的路径**（`io.github.u2894638479.kotlinmcui.test.TestTransformPage`），改动变换相关代码后应当跑一次；`io.github.u2894638479.kotlinmcui.test.TestPolygonPage` 是唯一走 `DslBackendTriangles` 这条可选能力的页面，改动顶点提交路径后应当跑一次它。

页面本身是**框架里公开的顶层 `context(DslContext)` 函数**，时序与判定也都在框架里——框架的预览会话负责解析引用、等首帧、按帧等待、判定、截图与退出码，后端只提供 `capture`（把当前帧写成文件）与 `quit`（结束游戏）两个平台动作。换一个后端时这两处不必重写。

日常验证在 `fabric-1.20.1` 分支上做：预览机制只在这一个分支上维护（迭代期其他版本分支不追）。在 `fabric-26.2` 上，自检还**建议用 Vulkan 再跑一遍**：

```
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestColorPage" ./gradlew runClient --args="--graphicsBackend vulkan"      # PowerShell 同样写法
```

Vulkan 对管线校验比 OpenGL 严格得多，绘制路径的问题它会更早暴露（例如"在游戏加载完成前就画 HUD"会让它在原版 shader 缓存就绪前编出无效管线，直接崩掉——细节见[开发文档](../dev/backend-fabric)）。OpenGL 能跑通不代表 Vulkan 能。

判定标准有两条：截图内容正常（说明界面确实画出来了），以及日志里有 `input callbacks registered`（说明安装输入回调的那处注入生效了——它失效时游戏照常启动，只是框架收不到任何输入，很容易被当成"界面不响应"）。

**截图只用来人眼看，不要拿它做像素比对。** 背景是动态的（模糊背景取自会转动的全景图），逐像素差异既可能来自渲染改动，也可能只是采样时刻不同，用它判断"改动是否生效/是否是 bug"会得出错误结论。

这个检查需要真实的显示与 GPU，无法在无头环境里运行。

## 手动检查清单
需要人眼确认时，按这个顺序看：

1. 模组列表里同时有 `kotlinmcui` 与 `kotlinmcuibackend`；
2. 配置页能打开，且列出了注册的 GUI 入口；
3. 任意入口点进去，界面可交互（按钮有悬停高亮、滑条可拖动、文本可输入）；
4. 日志里没有 `Exception` 与 mixin 注入失败。

## 常见故障
- **解析不到 Mod Menu**：它的仓库地址必须指向 `https://maven.terraformersmc.com/releases/`；旧文档里常见的 `/repository/maven-terraformers/` 路径会返回 404，导致整个配置阶段失败。
- **解析到了 JitPack 上的旧版本**：两个仓库的版本号不一致，见上文。
- **切换 Minecraft 版本后 `--offline` 失败**：Loom 的缓存是按游戏版本分的，第一次必须联网。
- **找不到 Java 25**：构建用工具链固定到 Java 25，本机没有时 Gradle 需要能自行解析（或在 `gradle.properties` 里配置工具链下载源）。
- **mixin 注入失败**：游戏能启动但某个功能失效，日志里会有 `Mixin apply failed`。这类失败通常是目标方法改名或改签名，见[移植](porting)。

## 发布
发布由标签驱动：`publishMods` 只在目标标签**尚不存在于 origin** 时才启用，标签名是 `v<版本号>`（`+` 换成 `-`）。发布类型由版本号里的关键字决定（`alpha` / `beta` / 其它视为正式）。上传到 Modrinth、CurseForge 与 GitHub Release 各需要一个环境变量里的 token；没有 token 时不要执行发布任务。
