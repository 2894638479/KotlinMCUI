# KotlinMCUI

使用 Kotlin DSL 编写的 Minecraft 模组 GUI 框架。用写 Kotlin 代码的方式写 UI。

[完整文档](https://2894638479.github.io/KotlinMCUI/) | [框架源码](https://github.com/2894638479/KotlinMCUI/) | [后端源码](https://github.com/2894638479/KotlinMCUI-backend/)

[![JitPack](https://jitpack.io/v/2894638479/KotlinMCUI.svg)](https://jitpack.io/#2894638479/KotlinMCUI)
[![backend](https://jitpack.io/v/2894638479/KotlinMCUI-backend.svg)](https://jitpack.io/#2894638479/KotlinMCUI-backend)

## 特点

- **纯 Kotlin DSL**。用 `if`/`else`、`for`、`when` 等原生语法控制 UI，不需要学习模板语言或 XML
- **不依赖 Minecraft 内部代码**。核心框架是一个纯 Kotlin 库，可以在没有 Minecraft 的环境中编译和测试
- **前后端分离**。渲染、输入、音频等平台相关功能由后端实现，框架只负责布局和事件分发。切换 GLFW / SDL3 只需修改后端
- **每帧自动刷新**。不需要手动维护 UI 状态——UI 树每帧重建，状态变化自动反映到界面上
- **类型安全**。编译期检查所有类型，重构时 IDE 可以自动更新所有引用

## 一个简单的例子

```kotlin
fun myScreen() = showScreen("示例") {
    Column {
        Text { "你好，世界！".emit() }
        Row {
            Button {
                Text { "点击我".emit() }
            }.clickable {
                println("按钮被点击了")
            }
            Spacer(width = 10.scaled)
            Button {
                Text { "关闭".emit() }
            }.clickable {
                closeScreen()
            }
        }
    }
}
```

## 快速开始

在 `build.gradle.kts` 中添加依赖：

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.2894638479:KotlinMCUI:$version")
}
```

然后创建一个后端入口（以 Fabric 为例）：

```kotlin
class MyMod : ClientModInitializer {
    override fun onInitializeClient() {
        dslBackendProvider = { MyBackend }
    }
}
```

完整教程请参阅 [文档](https://2894638479.github.io/KotlinMCUI/)。

## 组件一览

| 类别 | 组件 |
|---|---|
| 布局 | `Box`, `Row`, `Column`, `Spacer`, `ShrinkBox`, `LateBox` |
| 滚动 | `ScrollableColumn`, `ScrollableRow`, `LazyColumn`, `ScrollBar` |
| 控件 | `Button`, `Slider`, `EditableText` |
| 显示 | `Text`, `Image`, `Item`, `ColorRect` |
| 浮层 | `Overlay`, `MouseTip` |

## 装饰器

装饰器通过链式调用给组件添加行为和外观：

```kotlin
Button { Text { "删除".emit() } }
    .clickable { /* 处理点击 */ }
    .highlightBox()       // 获得焦点时显示边框
    .tooltip { Text { "确认删除".emit() } }
    .animateHeight()      // 高度变化时播放动画
```

## 项目结构

| 模块 | 说明 |
|---|---|
| `kotlinmcui` | 核心框架——布局算法、事件系统、DSL 语法、数学类型 |
| `kotlinmcui-backend` | 后端实现——将框架连接到具体的渲染平台（Minecraft + GLFW） |

## 开发后端

如果你想把框架适配到新的平台（如 SDL3），只需实现 `DslBackend` 接口。阅读 [后端开发文档](https://2894638479.github.io/KotlinMCUI/advanced/create-backend/) 了解详情。

## 许可

MIT License

---

# KotlinMCUI

*A Minecraft mod GUI framework powered by Kotlin DSL. Write UI as Kotlin code.*

[Documentation](https://2894638479.github.io/KotlinMCUI/) | [Framework](https://github.com/2894638479/KotlinMCUI/) | [Backend](https://github.com/2894638479/KotlinMCUI-backend/)

### Why KotlinMCUI?

- **Pure Kotlin DSL** — use `if`, `for`, `when` natively. No templates, no XML.
- **Minecraft-independent core** — the framework compiles and tests without Minecraft. Only the backend touches platform code.
- **Decoupled architecture** — swap rendering backends (GLFW → SDL3) without touching the framework.
- **Automatic refresh** — the UI tree rebuilds each frame. State changes are reflected automatically.
- **Type-safe** — compile-time checks, IDE refactoring support.

The [full documentation](https://2894638479.github.io/KotlinMCUI/) covers getting started, components, advanced customization, and backend development.
