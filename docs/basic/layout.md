---
title: Layout
nav_order: 2
parent: Basic
---
# 布局
布局写法和`Jetpack Compose`类似。用`Box{}`表示层叠，`Row{}`表示行，`Column{}`表示列。

布局过程中先完成所有组件的横向布局，再完成所有组件的纵向布局，因为部分组件需要计算文字换行所需的纵向空间。
## `Box{}`
```kotlin
Box {
    ColorRect(Modifier.padding(left = 10.scaled, top = 10.scaled),color = Color.RED) {}
    ColorRect(Modifier.padding(right = 10.scaled, bottom = 10.scaled),color = Color.BLUE) {}
}
```
更靠前的组件会排在更上层。右下角红色矩形在上层，左上角蓝色矩形在下层。

`createScreen{}`和`showScreen{}`默认也是`Box{}`。`Button{}`也是一种特殊的`Box{}`，只是额外渲染了背景。

## `Row{}`
```kotlin
Row {
    ColorRect(color = Color.RED) {}
    ColorRect(color = Color.GREEN) {}
    ColorRect(color = Color.BLUE) {}
}
```
三个色块会横向排列，并平均分配空间。

## `Column`
```kotlin
Column {
    ColorRect(color = Color.RED) {}
    ColorRect(color = Color.GREEN) {}
    ColorRect(color = Color.BLUE) {}
}
```
三个色块会纵向排列，并平均分配空间。

在默认情况下，父组件总是会被子组件填满并平分空间。后面会讲解如何更细致的调整空间分配。