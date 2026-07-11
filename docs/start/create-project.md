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
    implementation "com.github.2894638479:KotlinMCUI:v1.0.0-alpha.5"
    modImplementation "com.github.2894638479:KotlinMCUI-backend:v1.0.0-alpha.3-fabric-1.20.1"
}
```
`fabric-1.20.1`替换成你需要的分支。如果不是`fabric`，要用`implementation`代替`modImplementation`。

适配情况查看：

[KotlinMCUI source](https://github.com/2894638479/KotlinMCUI/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI.svg)](https://jitpack.io/#2894638479/KotlinMCUI)

[KotlinMCUI backend source](https://github.com/2894638479/KotlinMCUI-backend/)

[![KotlinMCUI JitPack Status](https://jitpack.io/v/2894638479/KotlinMCUI-backend.svg)](https://jitpack.io/#2894638479/KotlinMCUI-backend)
## loader中的依赖
在`fabric.mod.json`或`mods.toml`等文件中添加依赖。具体到对应加载器官网了解。

你只需要依赖`kotlinmcui`，因为`kotlinmcui`会依赖`kotlinmcuibackend`。
