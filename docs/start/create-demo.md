---
title: Create a Demo
nav_order: 3
parent: Start
---

# 创建一个Demo
## 创建`Screen`
首先以`TestPage()`作为内容进行演示。
如果要显示一个`Screen`，可以编写如下代码：
```kotlin
showScreen {
    TestPage()
}
```
这会直接调用`setScreen()`，让界面出现在画面上。

`showScreen`是一个顶层函数，需要在`DslExecuteContext`环境中调用，例如写在`clickable {}`等回调里。

如果要获得创建的`Screen`对象，如下：
```kotlin
val screen = root.screenFactory.create("DSL Screen") {
    TestPage()
}.screen as Screen
```
`create`的第一个参数是`Screen`的标题。这里必须进行类型转换，因为要跨版本支持。

不要在mod初始化过程中执行上述代码。那时平台还没发布（`DslRuntime.start`之前），建不了界面。使用按键或者`modmenu`等入口点触发这些代码。
## 创建自己的`Page`
只需要用自己的内容替换`TestPage()`即可。一个简单的示例：
```kotlin
showScreen {
    Button(Modifier.size(60.scaled,20.scaled)) {}
    DefaultBackground()
}
```
你将看到一个深色泥土背景（这取决于后端）和正中间的一个按钮。这个按钮不可点击。

在下一章节会开始更复杂、功能更强的ui界面。
