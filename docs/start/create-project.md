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

适配情况查看：

[KotlinMCUI source](https://github.com/2894638479/KotlinMCUI/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI.svg)](https://jitpack.io/#2894638479/KotlinMCUI)

[KotlinMCUI backend source](https://github.com/2894638479/KotlinMCUI-backend/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI-backend.svg)](https://jitpack.io/#2894638479/KotlinMCUI-backend)
## loader中的依赖
在`fabric.mod.json`或`mods.toml`等文件中添加依赖。具体到对应加载器官网了解。

安装时要同时放入`kotlinmcui`与`kotlinmcuibackend`两个模组，后端会精确校验核心库的版本。你自己的模组只依赖`kotlinmcui`就够了。
