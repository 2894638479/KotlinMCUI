---
title: Events
nav_order: 1
parent: Custom
---

# 事件
`DslComponent`由多个接口组成，其中事件相关的有`DslComponentEvent`，`DslComponentGlobalEvent`两个接口。大部分情况下你应该优先override `DslComponentEvent`中的函数来处理事件。

事件会从上到下依次传递。`children`中靠前的位于上层。
## 鼠标/键盘
`KeyDown`，`KeyUp`，`MouseDown`，`MouseUp`，`charTyped`，`dropFiles`的返回值是`Boolean`。如果你返回了`true`，事件将不会再继续传递下去。

`MouseMove`无返回值。他会始终传递下去。

`mouseScrollVertical`/`mouseScrollHorizontal`接收一个`Double`参数，返回一个`Double`。参数代表滚轮剩余的滚动量，返回值代表处理后剩余的滚动量。如果消耗了所有滚动量那么应该返回`0.0`，如果没有处理应该原样返回参数。如果已经返回了0，事件不会继续传递。

## 渲染
`render`中处理渲染事件。如果要调用`children`的`render()`，应该加上`asReversed()`，因为先绘制的内容会处于画面底层。如果要限制绘制的范围，可以使用`backend.withScissor`。


## 全局事件
`DslComponentGlobalEvent`中的事件，它们不需要手动向下传递，而是统一从根节点向下调用。