---
title: Property
nav_order: 7
parent: Basic
---

# 局部属性
```kotlin
var i by remember(0)

Button {
    TextFlatten(Modifier.padding(7.scaled)) { "counter: $i".emit() }
}.clickable { i++ }
```
这段代码生成了一个按钮，每次点击它，显示的数值就会+1。

