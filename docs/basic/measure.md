---
title: Measure
nav_order: 3
parent: Basic
---

# `Measure`
`Measure`是表示长度的统一类型。控件的长宽、边距，字体的大小，都应该用`Measure`表示。底层使用`value class`包装了`Double`，几乎没有开销。

## 如何创建一个Measure
```kotlin
10.px
10.scaled
10.5.px
10f.px
```
`.px`表示实际渲染的像素数目。

`.scaled`表示尺寸随着`minecraft`的ui缩放倍率缩放。只能在有`DslScaleContext`的环境使用。你也可以把缩放倍率换成一个自定义的倍率。

## 特殊值
```kotlin
Measure.AUTO
Measure.AUTO_MIN
```
它们的底层值是`Double`的`sNaN`中取出的特殊值。在运算时如果包含`NaN`值，结果会跟随出现的第一个`NaN`值。例如`AUTO_MIN + 1.px = AUTO_MIN`，`AUTO + AUTO_MIN = AUTO`。

