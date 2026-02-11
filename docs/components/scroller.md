---
title: Scroller
nav_order: 3
parent: Components
---

# 滚动
可滚动的控件（`ScrollableColumn`、`LazyColumn`）和滚动条（`ScrollBar`）是分开的。如果需要使用，应该用`Row`或`Column`把它们排在一起。

`ScrollableRow`目前还没有实现。
## `Scroller`
可以向`ScrollableColumn`传入`scrollerProp`和`scrollProp`。
```kotlin
val scrollerProp by Scroller.empty.remember.property
val scrollProp by 0.0.remember.property
LazyColumn(Modifier,scrollerProp,scrollProp) {
    // ...
}
ScrollBarVertical(Modifier.width(20.scaled),scrollerProp) {}
```
`scrollProp`不建议手动读写。它的作用是可以声明为`animatable()`来使滚动有平滑动画，并调整插值函数和速度。

`scrollerProp`是用来连接到`ScrollBar`等其它控件的。它的初始值是`Scroller.empty`，但是一帧后会被`LazyColumn`付上有效值。可以手动调用`Scroller.scrollTo()`等函数来控制滚动状态。

一个`LazyColumn`可以配多个`ScrollBar`，也可以横竖互相搭配。