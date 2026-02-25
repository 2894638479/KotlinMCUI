---
title: LateBox
nav_order: 6
parent: Components
---

# `LateBox`
用来实现更复杂的响应式布局。用它可以做出市面上常见的那种响应式布局demo（窄/宽/超宽）
```kotlin
LateBox {
    if(width > height) {
        //...
    } else {
        //...
    }
    
    if(width > 300.scaled) {
        //...
    }
}
```
内部有一个`Rect`作为接收者。可以读取`width`，`height`等属性来控制内部执行流。

## 实现
通过把`build`延迟到`LateBox`的`layoutVertical`阶段执行。此时已经知道了`LateBox`自身的宽高。

代价是`LateBox`内部的元素设置`Modifier.minSize()`并不能撑起`LateBox`自身的大小，因为它们被添加前`LateBox`已经完成布局。