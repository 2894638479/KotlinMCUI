---
title: Create a Project
nav_order: 2
parent: Start
---

# 创建项目

## 正在维护中，本页面中的内容可能不会工作。

在[fabric](https://fabricmc.net/develop/)/[forge](https://docs.minecraftforge.net/)/[neoforge](https://docs.neoforged.net/)或其它加载器的官网创建一个项目。

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
或者[modrinth文档](https://support.modrinth.com/en/articles/8801191-modrinth-maven)更推荐的写法：
```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}
```

## 添加依赖
然后，在`dependencies`中添加本模组作为依赖：
```groovy
dependencies {
    modImplementation "maven.modrinth:kotlinmcui:1.0.0-SNAPSHOT"
}
```
如果不是`fabric`，可能要用`implementation`代替`modImplementation`。
## 添加后端（开发环境中）
可以用同样的方式从`maven`中添加后端模组。这里也可以替换成其它第三方`backend`。
```groovy
dependencies {
    modImplementation "maven.modrinth:kotlinmcui-backend:1.0.0-SNAPSHOT+1.20.1"
}
```
还可以直接把模组放在运行目录`run/mods`中。但是这种方式不方便`git`追踪，所以只建议临时使用。

后端模组要区分`minecraft`版本和加载器。如果没有所需版本的后端，需要额外制作对应的后端。