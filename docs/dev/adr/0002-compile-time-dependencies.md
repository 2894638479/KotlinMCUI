# 核心库只声明编译期依赖

核心库用到 slf4j、fastutil、kotlinx-coroutines、kotlinx-serialization 与 kotlin-reflect，但它们一个都不进发布元数据的运行期依赖：平台已经带着它们，库再声明一份就是把第二份塞进消费者的类路径，和平台内嵌的那份撞车。于是按"公开签名里出现过没有"分两级——出现过的（slf4j、coroutines、serialization-core）用 `compileOnlyApi` 进消费者的**编译**类路径，只出现在实现里的（fastutil、serialization-json）用 `compileOnly`；两者都不写进运行期依赖。运行期由谁提供写进了[后端移植清单](../backend/porting)：Minecraft 自带 slf4j 与 fastutil，加载器的 Kotlin 语言提供者内嵌其余，缺库的加载器由后端把库打进自己的 jar。

## 被否的备选

- **`implementation`**（原行为）：依赖进消费者的运行期类路径。Fabric 上会与 Fabric Language Kotlin 内嵌的 coroutines/serialization 各存一份，而 slf4j 与 fastutil 本来就是 Minecraft 自带的。
- **`api`**：Kotlin 插件本来就提供这个配置，但它在塞进编译类路径的同时也塞进运行期元数据，等于退回上一条。
- **把库打进 jar（fat jar 或 shadow）**：消费者永远拿到两份，而且核心 jar 的字节会随依赖版本变化。

## 后果

- 纯 JVM 环境（例如只写单元测试）里用到公开签名中的成员——`dslLogger`、收 `CoroutineScope` 的回调、收 `StringFormat` 的配置工具——需要自己声明这些依赖。测试源码集因此对五个依赖都加了 `testImplementation`：`compileOnly` 不进测试类路径。
- 编译版本钉在**平台地板**（Minecraft 1.20.1 带的 slf4j 2.0.1 与 fastutil 8.5.9）。编译面取"声称支持的最旧运行期"，才不会用上玩家那儿根本没有的 API。
- 换一个加载器时首先要回答"这些库由谁提供"，这是移植清单的第一项，而不是等运行期抛 `NoClassDefFoundError` 再查。
