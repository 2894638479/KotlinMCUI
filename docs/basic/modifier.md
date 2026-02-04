---
title: Modifier
nav_order: 4
parent: Basic
---

# `Modifier`
相比`Jetpack Compose`,这里的`Modifier`实现的简单得多。它只是一个存储了布局所需信息的类。

```kotlin
Modifier.padding(10.scaled)
Modifier.weight(2.0)
Modifier.align { left().middleY() }
Modifier.minSize(60.scaled,40.scaled)
Modifier.size(40.scaled,20.scaled)
Modifier.height(25.scaled)
```
一些常用用法。也可以连接起来，先后顺序无关。
```kotlin
Modifier.weight(2.0).align { left().middleY() }.minSize(60.scaled,40.scaled)
```

## 布局规则
- `size`不为`NaN`：尺寸固定为`size`。
- `size`为`AUTO_MIN`：尺寸固定为`minSize`，如果`minSize`为`NaN`则尺寸为`0.px`。
- `size`为`AUTO`或其它`NaN`：尺寸由父控件`Aligner`自行决定，一般是尽量填满并按照`weight`分配。

`size`表示在`Modifier`中指定的`width`或`height`。`minSize`由`Modifier`中指定的`minWidth`或`minHeight`，以及子控件排列后要求的最小尺寸共同决定。

不提供任何限制最大尺寸的选项，因为不常用且会造成很多逻辑冲突。