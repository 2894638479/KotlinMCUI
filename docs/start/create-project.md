---
title: Create a Project
nav_order: 1
parent: Start
---

# 创建项目

在[fabric](https://fabricmc.net/develop/)/[forge](https://docs.minecraftforge.net/)/[neoforge](https://docs.neoforged.net/)或其它加载器的官网创建一个`kotlin`项目。

## 添加`maven`仓库
以`gradle`构建系统（`groovy dsl`）为示例。在`repositories`块中添加：
```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```
## 添加依赖
在`dependencies`中添加本模组作为依赖：
```groovy
dependencies {
    implementation "com.github.2894638479:KotlinMCUI:v1.0.0-alpha.32"
    modImplementation "com.github.2894638479:KotlinMCUI-backend:v1.0.0-alpha.19-fabric-26.2"
}
```
上面示例用的是当前维护的分支`fabric-26.2`（Minecraft `26.2`、Fabric、Java 25）。另一个维护中的分支是`fabric-1.20.1`（Minecraft `1.20.1`、Fabric、Java 17），它上面的版本号与这里不同，要取该分支上成对发布过的版本。切换到其它分支时两个版本号必须一起换成该分支上成对发布过的版本，因为后端对核心库的依赖是精确版本，不匹配会在加载时直接报依赖不满足。其它Minecraft版本的分支不再更新；各分支上发布过哪些版本见上面的JitPack徽章。如果不是`fabric`，要用`implementation`代替`modImplementation`。

核心库不会把自己的运行期依赖带进你的工程：`slf4j-api`、`fastutil`、`kotlinx-coroutines-core`、`kotlinx-serialization-core`/`-json` 都由平台提供（Minecraft 自带前两个，Fabric Language Kotlin 内嵌后三个），它们在核心库的元数据里只对**编译期**可见。所以如果你在纯 JVM 环境（比如只写单元测试）里用到出现在公开签名中的成员——`dslLogger`、收 `CoroutineScope` 的回调、收 `StringFormat` 的配置工具——需要自己把它们加进依赖。

核心库也不限定 Minecraft 版本、加载器与环境：元数据里不写游戏版本区间、不认某个具体加载器，`java` 要求是 8 及以上。它对**后端**是硬依赖，因为 Kotlin 运行时由后端挑选提供（Fabric 上是 Fabric Language Kotlin，Forge/NeoForge 上是 Kotlin for Forge 或 KotlinLangForge）；服务端也可以有服务端专用的后端。**某个环境能不能用，取决于有没有对应的后端**——同一个核心库 jar 可以配任何加载器、任何游戏版本的后端。

适配情况查看：

[KotlinMCUI source](https://github.com/2894638479/KotlinMCUI/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI.svg)](https://jitpack.io/#2894638479/KotlinMCUI)

[KotlinMCUI backend source](https://github.com/2894638479/KotlinMCUI-backend/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI-backend.svg)](https://jitpack.io/#2894638479/KotlinMCUI-backend)
## loader中的依赖
在`fabric.mod.json`或`mods.toml`等文件中添加依赖。具体到对应加载器官网了解。

安装时要同时放入`kotlinmcui`与`kotlinmcuibackend`两个模组，后端会精确校验核心库的版本。你自己的模组只依赖`kotlinmcui`就够了。
