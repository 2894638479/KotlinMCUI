---
title: Create a Project
nav_order: 2
parent: Start
---

# 创建项目

在[fabric](https://fabricmc.net/develop/)/[forge](https://docs.minecraftforge.net/)/[neoforge](https://docs.neoforged.net/)或其它加载器的官网创建一个`kotlin`项目。

## 添加`maven`仓库
以`gradle`构建系统（`groovy dsl`）为示例。在`repositories`块中添加：
```groovy
repositories {
    maven {
        name "Modrinth"
        url "https://api.modrinth.com/maven"
    }
}
```

## 添加依赖
然后，在`dependencies`中添加本模组作为依赖：
```groovy
dependencies {
    modImplementation "com.github.2894638479:KotlinMCUI:master-SNAPSHOT"
}
```
如果不是`fabric`，可能要用`implementation`代替`modImplementation`。
## 添加后端（开发环境中）
可以用同样的方式从`maven`中添加后端模组。这里也可以替换成其它第三方`backend`。
```groovy
dependencies {
    modImplementation "com.github.2894638479:KotlinMCUI-backend:master-SNAPSHOT"
}
```
还可以直接把模组放在运行目录`run/mods`中。但是这种方式不方便`git`追踪，所以只建议临时使用。

后端模组要区分`minecraft`版本和加载器。如果没有所需版本的后端，需要额外制作对应的后端。

## 开启`context parameters`
```groovy
tasks.withType(KotlinCompile).configureEach {
    compilerOptions.freeCompilerArgs.add('-Xcontext-parameters')
}
```