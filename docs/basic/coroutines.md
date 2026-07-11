---
title: Coroutines
nav_order: 8
parent: Basic
---
# 协程
`dslBackend.mainDispatcher`提供的调度器可以把任务调度到`minecraft`的主线程（渲染线程）上。

## 通过Context启动
部分回调包含`CoroutineScope`接收者。可以使用`launch{}`，`async{}`，`withContext(){}`等扩展函数。

默认在渲染线程执行。如果需要做耗时计算、IO读写，需要手动指定在`Dispatchers.IO`。

