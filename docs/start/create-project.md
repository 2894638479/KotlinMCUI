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
    implementation "com.github.2894638479:KotlinMCUI:master-SNAPSHOT"
    modImplementation "com.github.2894638479:KotlinMCUI-backend:fabric-1.20.1-SNAPSHOT"
}
```
`fabric-1.20.1`替换成你需要的分支。如果不是`fabric`，要用`implementation`代替`modImplementation`。

适配情况查看：

[kotlinmcui source](https://github.com/2894638479/KotlinMCUI/)

[kotlinmcui jitpack](https://jitpack.io/#2894638479/KotlinMCUI)

[kotlinmcui-backend source](https://github.com/2894638479/KotlinMCUI-backend/)

[kotlinmcui-backend jitpack](https://jitpack.io/#2894638479/KotlinMCUI-backend)
## 开启`context parameters`
这可能需要比较高的`kotlin`版本。
```groovy
tasks.withType(KotlinCompile).configureEach {
    compilerOptions.freeCompilerArgs.add('-Xcontext-parameters')
}
```