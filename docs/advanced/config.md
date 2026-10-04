---
title: Config
nav_order: 2
parent: Advanced
---

# 配置文件
`saveConfig` 与 `loadConfig` 把任意 `@Serializable` 对象存成配置文件。格式由调用方给（`StringFormat`），框架自带一个 `configFormat`（带缩进的 JSON）：

```kotlin
@Serializable
data class MyConfig(val volume: Int = 3, val order: List<String> = emptyList())

saveConfig("mymod/settings.json", MyConfig(volume = 5), configFormat)   // <configDir>/mymod/settings.json
val config = loadConfig("mymod/settings.json", configFormat) { MyConfig() }
```

- 按名字存取的版本放在后端的配置目录下；要放到别处就直接传 `Path`。
- **读不到就用默认值**：文件不存在、读不了、解析失败都会回落到末尾 lambda 给的默认值并记一条日志。首次启动不需要额外判断。
- **写入是原子替换**：先写临时文件再移动，中途崩溃不会留下半个配置文件；写失败只记日志，不抛出。
- 序列化器可以显式传入。只有在泛型参数 `T` 无法 reified 时才需要（例如调用方自己也是泛型函数、手上只有一个 `KSerializer<T>`）；`List<Int>` 这类具体类型直接用上面的写法即可。

框架自己用这套机制保存已启用的 Overlay 列表。
