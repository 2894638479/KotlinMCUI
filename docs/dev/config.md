---
title: 配置
nav_order: 15
parent: Dev
---

# 配置
框架需要一个能存放用户设置的地方，于是有了一套极小的配置文件读写工具：`saveConfig` / `loadConfig`。

## 为什么是"整个对象序列化"
最初想做的是 `getString` / `setInt` / `getStringList` 那样的门面，但那没有简化任何东西：键名、类型、默认值、文件布局仍然全部由调用方维护，只是把 `Map` 换了个包装。现在的形状是——调用方声明一个 `@Serializable` 数据类，一行存、一行读，键名根本不存在：

```kotlin
@Serializable
data class MyConfig(val volume: Int = 3, val order: List<String> = emptyList())

saveConfig("mymod/settings.json", MyConfig(volume = 5), configFormat)
val config = loadConfig("mymod/settings.json", configFormat) { MyConfig() }
```

## 格式是参数
`StringFormat` 由调用方传入，工具本身不认识 JSON——核心库因此只编译依赖 `kotlinx-serialization-core`。框架自己的文件用 `configFormat`（`Json`，`prettyPrint` + `ignoreUnknownKeys`），所以**运行期**需要 `kotlinx-serialization-json`：Fabric 上由 Fabric Language Kotlin 提供（其打包的版本与核心编译用的 1.11.0 一致），缺库的加载器由后端负责把库打进 jar。

## 三条约定
- **写入**：先写到目标旁边的临时文件，再原子替换（个别文件系统不支持原子移动时退回普通替换）。失败只记日志、不抛出——保存一个设置不该把游戏带崩。
- **读取**：文件不存在、读不了、解析失败，一律回落到调用方给的默认值并记日志。首次启动本来就没有文件；手改坏了配置也只该损失设置，不该拦住启动。
- **路径**：按名字存取的版本解析到 `dslBackend.configDir`，框架自己的文件放在 `kotlinmcui/` 子目录里，避免和模组自己的配置混在一起。

## 框架自己的用法
入口这一层用它保存**已启用的 Overlay 列表**：`<configDir>/kotlinmcui/overlays.json`，内容是有序的 `DslEntryService.id` 数组。详见[入口](entry)一章的 Overlay 部分。
