---
title: Coroutines
nav_order: 8
parent: Basic
---
# 协程
每个根都有一条会话线程，它有两个入口：

- `root.sessionScope`：**要挂并发的协程用它**（`SupervisorJob + sessionDispatcher`，随 `root.close()` 取消，不会活得比树更久）；
- `root.post { }`：**非挂起的回调**用它。已经在会话线程上就**内联**执行，否则投过去；异常不吞。跨线程投递没有顺序保证，别拿它和帧循环排顺序。

底层原语是 `DslRootUtils.sessionDispatcher`（串行调度器，指向该根的主线程）与 `isOnSessionThread`。注意自己用 `Minecraft.getInstance()` 之类另造一个 dispatcher 指到同一线程是**错的**：框架的 `isOnSessionThread`、`session` 属性作用域都绑在会话调度器上，自己造的那个在框架看来是"别的线程"。

## 通过Context启动
部分回调包含`CoroutineScope`接收者。可以使用`launch{}`，`async{}`，`withContext(){}`等扩展函数。

默认在渲染线程执行。如果需要做耗时计算、IO读写，需要手动指定在`Dispatchers.IO`。

