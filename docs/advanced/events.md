---
title: Events
nav_order: 1
parent: Custom
---

# 事件
`DslComponent`由多个接口组成，其中事件相关的有`DslComponentEvent`，`DslComponentGlobalEvent`两个接口。大部分情况下你应该优先override `DslComponentEvent`中的函数来处理事件。

事件会从上到下依次传递。`children`中靠前的位于上层。
## 鼠标/键盘
`keyDown`，`keyUp`，`mouseDown`，`mouseUp`，`charTyped`，`dropFiles`的返回值是`Boolean`。如果你返回了`true`，事件将不会再继续传递下去。

`mouseMove`无返回值。他会始终传递下去。

`mouseScrollVertical`/`mouseScrollHorizontal`接收一个`Double`参数，返回一个`Double`。参数代表滚轮剩余的滚动量，返回值代表处理后剩余的滚动量。如果消耗了所有滚动量那么应该返回`0.0`，如果没有处理应该原样返回参数。如果已经返回了0，事件不会继续传递。

## 渲染
`render`中处理渲染事件。如果要调用`children`的`render()`，应该加上`asReversed()`，因为先绘制的内容会处于画面底层。如果要限制绘制的范围，可以使用`backend.withScissor`。


## 全局事件
`DslComponentGlobalEvent`中的事件，它们不需要手动向下传递，而是统一从根节点向下调用。

## 焦点
焦点保存在**根**（`DslRoot`）上：它属于那个根的树，因此是每根的状态——同一进程里的多个后端各有各的焦点，互不干扰。组件覆写`focusable`声明自己能否获得焦点，用`isFocused`查询，用`onFocused`/`onFocusChanged`（或`globalFocusChanged`）观察变化。

焦点由鼠标与键盘导航驱动，它们与代码走的是根内部同一个入口。由代码主动设焦点的唯一公开入口是`root.setFocus(identity)`，`null`表示清除；`source`默认就是`FocusSource.CODE`，需要区分"是谁设的焦点"时显式给。`DslRootState`保持`internal`，所以只有这一个成员是公开的。

有几条语义值得注意：`root.setFocus`**不**筛选`focusable`，一个不能获得焦点的目标照样会被记录，只是它的`isFocused`仍为`false`；`identity`（`DslId`）是结构化比较的，所以上一帧取到的`DslChild`这一帧依然能匹配到重建后的组件；焦点变化从根节点向下**后序**广播（子组件先知道，父容器最后），与键盘导航一致。