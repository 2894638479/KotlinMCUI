---
title: Entrypoint
nav_order: 4
parent: Start
---
# 入口点
`kotlinmcui`提供一个加载器无关的入口点，并且二进制兼容。意味着你可以不再依赖`fabric/forge/neoforge`等加载器提供的入口点。

入口点有很多接口，你可以根据需求进行选择。
## 用法
### 入口点类编写
入口类继承对应的接口即可。可以同时继承多个接口。

更推荐使用`object`而不是`class`，虽然二者都能处理。
```kotlin
object Entry : DslEntryCommon, DslEntryGui
```
### service注册
创建`resources/META-INF/services/io.github.u2894638479.kotlinmcui.backend.DslEntryXXX`文件，向其中写入注册类的全限定名。
其中的`XXX`替换为你需要注册的接口。

文件内容示例
```text
a.b.c.Entry
a.b.c.Entry$Client
```
这一写法与java的标准service是相同的，但是为了兼容`kotlin object`，没有直接使用`ServiceLoader`进行加载。
### Client与Server区分
`forge`和`fabric`等加载器都会对环境进行区分，因为某些类只存在于Client或Server其中一侧。防止引用到不存在的类（在类加载时就直接崩溃）。
```kotlin
object Entry : DslEntryCommon, DslEntryGui, DslEntryOverlay {
    override fun initialize() {}
    // ...

    // 包含对Client特有类的调用
    object Client : DslEntryClient {
        override fun initializeClient() {}
    }

    // 包含对Server特有类的调用
    object Server : DslEntryServer {
        override fun initializeServer() {}
    }
}
```
展示了一种推荐写法。你也可以把这些类完全拆开，甚至放入不同源集。注意每个类都要注册在service中。

把在Client环境会加载`DslEntryCommon`和`DslEntryClient`，
在Server环境会加载`DslEntryCommon`和`DslEntryServer`。从而避免引用到不存在的类。

注意，以上区分仅对运行环境进行区分，Server环境没有`minecraft`的渲染相关类，不表示无法显示gui。Server环境下仍可能通过其它后端实现ui渲染，因此Gui和Overlay仍然会被加载。**因此，如果一个入口类同时继承了`DslEntryClient`和`DslEntryGui`，那么在Server环境下虽然`initializeClient()`不会被执行，但是这个类会被加载。**

## 接口
目前有5种。分别为Client、Server、Common、Gui、Overlay。
### Client
需要实现`DslEntryClient.initializeClient()`。此为逻辑代码执行的入口点。在Client环境下会被调用一次。
### Server
需要实现`DslEntryServer.initializeServer()`。此为逻辑代码执行的入口点。在Server环境下会被调用一次。
### Common
需要实现`DslEntryCommon.initialize()`。此为逻辑代码执行的入口点。只要注册了，在任何环境下都会被调用一次。
### Gui
需要实现`DslEntryGui.content()`。函数需要实现ui内容。

通过此方式注册的gui会显示在`kotlinmcui-backend`的配置页面中，点击可以进入这个界面对应的`Screen`。
```kotlin
context(ctx: DslContext)
override fun content() {
    Row {
        Column {
            // ... 
        }
        Button {
            // ... 
        }
    }
    DefaultBackground {}
}
```
### Overlay
需要实现`DslEntryOverlay.overlay()`。实现内容同上。

```kotlin
context(ctx: DslContext)
override fun overlay() {
    // ... 
}
```
通过此方式注册的overlay会在游戏界面顶层全屏叠加渲染，并且可以处理鼠标键盘等事件。

需要在`kotlinmcui-backend`的配置页面内部手动开启后才会显示。
