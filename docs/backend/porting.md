---
title: Porting
nav_order: 3
parent: Backend
---

# 移植到新的游戏版本

一个分支只对应**一个加载器 + 一个 Minecraft 版本**。升级时另开分支，不要在一个分支里同时兼容两个版本。

## 1. 先确认运行期依赖由谁提供
核心库把这些依赖都当**编译期**依赖，运行期必须由平台提供：`slf4j-api` 与 `fastutil` 由 Minecraft 自带（它们是启动器库，不在 merged jar 里），`kotlin-stdlib`、`kotlinx-coroutines-core`、`kotlinx-serialization-core`/`-json` 与 `kotlin-reflect` 由加载器的 Kotlin 语言提供者内嵌。目标加载器缺哪个，就由**后端**把它打进自己的 jar，否则核心库会在运行期抛 `NoClassDefFoundError`。

同时要在加载器元数据里指向所用的 Kotlin 提供者：Fabric 用 `fabric-language-kotlin`，Forge 侧是 Kotlin for Forge，NeoForge 侧用它自己的变体或 KotlinLangForge。Forge 与 NeoForge 是靠 `modLoader` / `loaderVersion` 选择语言适配器的，写错或缺失会在解析适配器时就拒绝加载，而不是等到运行期。

## 2. 先改版本号
`gradle.properties` 里几乎每一项都要重新确认：

| 键 | 说明 |
| --- | --- |
| `minecraft_version` / `minecraft_version_range` | 目标游戏版本 |
| `loader` / `loader_suffix` | 加载器名与后缀，决定产物名的第二、三段 |
| `plugin_version` | Loom 版本，要能支持目标游戏版本 |
| `loader_version` / `loader_version_range` | Fabric Loader |
| `fabric_kotlin_version` | Fabric Language Kotlin，版本号要跟 Kotlin 版本对齐 |
| `mod_menu_version` | Mod Menu，可选依赖 |
| `java_version` / `toolkit_version` | 游戏要求的 Java 版本（26.2 是 25） |
| `kotlinmcui_version` / `kotlinmcui_version_range` | 核心库，必须与本地构建的 jar 一致 |

Loom 从某个版本起默认使用**官方命名**（Mojang 名），不再需要 yarn 映射；本仓库的代码与访问扩大器都按官方命名书写。

## 3. 访问扩大器
`kotlinmcuibackend.accesswidener` 用 `official` 命名空间打开一批私有成员。按失效风险从高到低：

- **构造器签名**：`Style` 的构造器与 `ItemStack` 的构造器都是把全部字段一次列出的长签名，只要游戏给这些类加字段就会失效，而且报错发生在运行期；
- **方法签名**：`GuiGraphicsExtractor.innerBlit(...)`（参数多、类型具体）、`Font.getGlyphSource(...)`；
- **字段与嵌套类**：`GuiGraphicsExtractor.guiRenderState`、同类的 `scissorStack` 与 `ScissorStack`、`AbstractButton.SPRITES`、`RenderPipelines.GUI_SNIPPET` 与 `GUI_TEXTURED_SNIPPET`；
- **注册入口**：`RenderPipelines.register(...)`，游戏重构渲染管线注册方式时会消失。

改写这些条目时要连**签名一起**改，不能只改名字。

## 4. Mixin
仓库有三处 Mixin，各自替换后失效的方式不同：

- **注册输入回调**：注入游戏主类的构造器返回处，拿到窗口句柄后安装 GLFW 回调。游戏若改变窗口初始化时机需要重新选注入点。
- **插入 HUD 渲染**：重定向 HUD 渲染过程中设置光标的那一步，把核心的界面画进去。**这是最依赖渲染模型的一处**——26.x 把 GUI 渲染改成了"先提取渲染状态、再统一绘制"，所以插入点必须跟着换；换版本时优先检查这里。
- **位姿矩阵的第三行**：目标是 JOML 的二维矩阵类（库类，不是游戏类），给它的仿射矩阵补上第三行，并把投影除法做在顶点变换里。需要跟着改的是：被注入的顶点变换重载、拷贝构造器、以及从另一个矩阵赋值的方法（原因见[开发文档](../dev/backend-fabric)的位姿矩阵一节）。

Mixin 配置里的兼容级别保持在与目标 Java 版本相符的值即可，不必跟着工具链一起抬高。

## 5. 渲染管线
后端自己注册两条管线（纯色三角形与带纹理三角形），做法是在游戏内置的 GUI 管线上覆盖图元拓扑并关闭面剔除。若游戏重命名了这些内置管线或改了构建器，这里要跟着改。

## 6. 怎么查"到底哪个 API 变了"
Loom 会把 remap 之后的游戏 jar 放在项目里，可以直接用 `javap` 查签名：

```
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-<哈希>/<版本>/minecraft-merged-<哈希>-<版本>.jar
```

例如确认一个类还剩哪些成员：
```
javap -classpath <上面那个 jar> net.minecraft.client.gui.Gui
```
比翻反编译源码快，而且拿到的是**当前实际依赖的那个版本**的签名。校验访问扩大器与 Mixin 目标时都用这个方法。

## 7. 移植完的验证
按[构建与运行](build)的顺序跑一遍：两个仓库都构建通过，`JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=io.github.u2894638479.kotlinmcui.test.TestTransformPage -Dkotlinmcui.preview.capture=true"` 配上 `./gradlew runClient` 时，变换演示页三种变换都正常并写出截图。透视那一页尤其重要——它是唯一依赖位姿矩阵第三行的路径。预览会话用 Kotlin 反射判断页面参数，核心库只把它当**编译期依赖**，运行期要由后端所在平台提供（Fabric 的语言加载器自带 `kotlin-reflect`）；平台没有它时，带额外参数的页面会被拒绝并写明原因。
