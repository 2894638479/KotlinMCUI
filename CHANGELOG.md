# Changelog

发布的正文从这里取：`publishMods` 的 `changelog` 读整个文件，所以**每次发版前在这里加一节**。

## v1.0.0-alpha.32

入口模型改成"一个接口 + 注册文件里的一列环境"：

- `DslEntryClient` / `DslEntryServer` / `DslEntryCommon` 合并为一个 `DslEntryService`（`initialize()` / `launch()`）；
- 一个入口在哪些环境加载，写在 `META-INF/kotlinmcui/entries` 里，**在加载类之前过滤**，因此声明为 `server` 的类在客户端里连类都不会被加载；
- `Environment.COMMON` / `NONE` 统一为 `STANDALONE`（没有游戏端，但不等于不加载入口）；
- 种类仍是接口：`DslEntryGui.gui()` 与 `DslEntryOverlay.overlay()`，所以"有没有界面"是类型事实，不需要额外声明；
- 两个 Overlay 声明同一个 id 时先声明者胜，后者作为坏条目显示在配置页；
- `launch()` 仍然由 `DslRuntime.launch()` 显式触发，幂等，平台未发布时抛错。
