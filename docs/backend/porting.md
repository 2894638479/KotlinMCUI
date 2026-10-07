---
title: Porting
nav_order: 3
parent: Backend
---

# 移植到新的游戏版本

一个分支只对应**一个加载器 + 一个 Minecraft 版本**。升级时另开分支，不要在一个分支里同时兼容两个版本。

## 1. 先确认运行期依赖由谁提供
核心库把这些依赖都当**编译期**依赖，运行期必须由平台提供：`slf4j-api` 与 `fastutil` 由 Minecraft 自带（它们是启动器库，不在 merged jar 里），`kotlin-stdlib`、`kotlinx-coroutines-core`、`kotlinx-serialization-core`/`-json` 与 `kotlin-reflect` 由加载器的 Kotlin 语言提供者内嵌。目标加载器缺哪个，就由**后端**把它打进自己的 jar，否则核心库会在运行期抛 `NoClassDefFoundError`。

同时要在**后端自己**的加载器元数据里指向所用的 Kotlin 提供者：Fabric 用 `fabric-language-kotlin`，Forge 侧是 Kotlin for Forge 或 KotlinLangForge，NeoForge 侧同理。核心库的元数据里不出现任何提供者的名字——运行时由后端挑选提供。

加载器的声明方式两边不同：**Forge** 必须写 `modLoader` / `loaderVersion`（Forge 文档明说这是必需的，写错或缺失会在解析适配器时就拒绝加载）；**NeoForge** 更麻烦——`26.x` 把它标为弃用、日志会建议删掉这两个设置，但 `21.1` 反过来要求必须有它，缺了直接报 `InvalidModFileException: Missing ModLoader`，整份 mod 文件被判无效。一份元数据要同时服务两条版本线时只能保留声明，代价是 `26.x` 上一条弃用警告。核心库就是这么取舍的。

还要确认**模组资源**由谁加载：Fabric 上这件事由 Fabric API 的资源加载器负责，缺了它打包后的客户端里核心库的语言文件读不到，所有翻译都会变成键名（开发环境由 Loom 代劳，所以只在打包后暴露）。后端因此在 `fabric.mod.json` 里把 `fabric-api` 写成依赖。Forge / NeoForge 原生就加载模组资源，不需要额外模组。

## 2. 先改版本号
`gradle.properties` 里几乎每一项都要重新确认：

| 键 | 说明 |
| --- | --- |
| `minecraft_version` / `minecraft_version_range` | 目标游戏版本 |
| `loader` / `loader_suffix` | 加载器名与后缀，决定产物名的第二、三段 |
| `plugin_version` | 构建插件的版本：Fabric 是 Fabric Loom，Forge 是 ForgeGradle，NeoForge 是 ModDevGradle |
| `loader_version` / `loader_version_range` | Fabric Loader（Forge/NeoForge 用的是它们自己的加载器版本） |
| `fabric_kotlin_version` | Fabric Language Kotlin，版本号要跟 Kotlin 版本对齐（Forge/NeoForge 用的是 `kff_version` / `klf_version`，同样要跟 Kotlin 对齐） |
| `mod_menu_version` | Mod Menu，可选依赖（只有 Fabric 有） |
| `java_version` / `toolkit_version` | 游戏要求的 Java 版本：`1.20.1` 是 17、`1.21.1` 是 21、`26.x` 是 25 |
| `kotlinmcui_version` / `kotlinmcui_version_range` | 核心库，必须与本地构建的 jar 一致 |

### 构建工具链与构建用 JDK
同一份代码在不同加载器上的可行组合差别很大，先按目标选工具链，再选 Gradle，最后选**构建用的 JDK**——三者互相约束：

| 加载器 | 构建插件 | Gradle | 构建用 JDK |
| --- | --- | --- | --- |
| Fabric（需要 remap 的旧版本） | Fabric Loom 1.13-SNAPSHOT + 旧 DSL | 9.8.0 | 游戏要求的版本 |
| Fabric（`26.x`） | Fabric Loom 1.18.2 + 新 DSL | 9.8.0 | 游戏要求的版本 |
| Forge | ForgeGradle `[6.0,6.2)`（实测解析到 6.0.54） | **8.14.5** | **17** |
| NeoForge | ModDevGradle 2.0.148 | 9.8.0 | 游戏要求的版本 |

Forge 那一行两个限制都是实测出来的：ForgeGradle 拒绝 Gradle 9（原文 `Versions Gradle 9.0 and newer are not supported yet`），而 Gradle 8.14 在 JDK 25 上自己起不来（`IllegalArgumentException: 25.0.2`），所以构建必须用 JDK 17——这与 CI 的 `setup-java` 一致。

Loom 从 1.17 起删掉了 remapping 那一套 DSL：`mappings` / `modImplementation` / `modCompileOnly` / `modLocalRuntime` / `remapJar` 都会解析失败。需要 remap 的旧分支因此必须留在 `id("fabric-loom")` + 旧 DSL，`26.x` 才用新插件 id 与新 DSL。

Loom 从某个版本起默认使用**官方命名**（Mojang 名），不再需要 yarn 映射；本仓库的代码与访问扩大器都按官方命名书写。

## 3. 访问扩大器
`kotlinmcuibackend.accesswidener` 用 `official` 命名空间打开一批私有成员。按失效风险从高到低：

- **构造器签名**：`Style` 的构造器与 `ItemStack` 的构造器都是把全部字段一次列出的长签名，只要游戏给这些类加字段就会失效，而且报错发生在运行期；
- **方法签名**：`GuiGraphicsExtractor.innerBlit(...)`（参数多、类型具体）、`Font.getGlyphSource(...)`；
- **字段与嵌套类**：`GuiGraphicsExtractor.guiRenderState`、同类的 `scissorStack` 与 `ScissorStack`、`AbstractButton.SPRITES`、`RenderPipelines.GUI_SNIPPET` 与 `GUI_TEXTURED_SNIPPET`；
- **注册入口**：`RenderPipelines.register(...)`，游戏重构渲染管线注册方式时会消失。

改写这些条目时要连**签名一起**改，不能只改名字。

**NeoForge 不读 access widener**：同一批成员要在 `META-INF/accesstransformer.cfg` 里用 Access Transformer 再写一遍（ModDevGradle 会自动取这个路径，元数据里再声明一次 `[[accessTransformers]]`）。两种写法里的名字与签名完全一致（都用官方命名），区别只在关键字：`accessible method/field/class` 对应 `public`，需要可赋值的用 `public-f`。逐条对着目标版本的 jar 用 `javap` 核过再写。

**别按名字猜哪个成员本来就是公开的。** 实测：把 `1.21.1` 上 `GuiGraphics.bufferSource` 与 `GuiGraphics.innerBlit(...)` 两条 AT 注释掉再编译，立刻报 `Cannot access ... it is private` / `it is package-private`——它们看着像公开（框架代码到处在调），实际全靠 AT 打开。反过来说也确有冗余条目：`26.3` 上 `RenderPipelines.GUI_SNIPPET` 与 `GUI_TEXTURED_SNIPPET` 本来就是 public，写进去只是为了与 Fabric 侧 access widener 一一对应。

## 4. Mixin
仓库有三处 Mixin，各自替换后失效的方式不同：

- **输入**：`1.20.1` / `1.21.1` 上是"注入游戏主类构造器返回处，拿到窗口句柄后安装 GLFW 回调"；`26.3` 起游戏改用 **SDL**（classpath 上不再有 `org.lwjgl.glfw`，merged jar 里 `org/lwjgl/**` 为空），没有平台回调可以接管，改为注入游戏自己的 `KeyboardHandler` 与 `MouseHandler`，在事件进入游戏前先交给框架、并决定是否 `cancel`。键码也随之变成 SDL scancode。换版本时先确认这一层还在不在。
- **插入 HUD 渲染**：重定向 HUD 渲染过程中设置光标的那一步，把核心的界面画进去。**这是最依赖渲染模型的一处**——26.x 把 GUI 渲染改成了"先提取渲染状态、再统一绘制"，所以插入点必须跟着换；换版本时优先检查这里。
- **位姿矩阵的第三行**：目标是 JOML 的二维矩阵类（库类，不是游戏类），给它的仿射矩阵补上第三行，并把投影除法做在顶点变换里。需要跟着改的是：被注入的顶点变换重载、拷贝构造器、以及从另一个矩阵赋值的方法（原因见[开发文档](../dev/backend-fabric)的位姿矩阵一节）。

Mixin 配置里的兼容级别保持在与目标 Java 版本相符的值即可，不必跟着工具链一起抬高。

## 5. 渲染管线
后端自己注册两条管线（纯色三角形与带纹理三角形），做法是在游戏内置的 GUI 管线上覆盖图元拓扑并关闭面剔除。若游戏重命名了这些内置管线或改了构建器，这里要跟着改。

**注册时机比注册方式更容易出错**：管线是在资源重载时编译的，而编译只覆盖那一刻已经注册的管线，所以两条管线必须在**首次资源重载之前**注册完。写成 `by lazy` 会在第一次画三角形时才注册，运行期直接抛 `Failed to find or load pipeline`——编译期完全看不出来。`26.x` 上 `RenderPipeline` 与 `PrimitiveTopology` 位于 `com.mojang.renderpearl.api.pipeline`。

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
