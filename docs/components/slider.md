---
title: Slider
nav_order: 5
parent: Components
---

# 滑条
支持`Int`和`Double`。支持键盘操作，让焦点位于滑条上（可以点一下，也可以用方向键或Tab键选中），然后按下enter，就能通过方向键调节值。

```kotlin
val valueProp by remember(30.0).property
val value by valueProp
SliderHorizontal(Modifier.height(20.scaled),0.0..100.0,valueProp) {
    TextFlatten { "value:$value".emit() }
}
```
如果想要监听值改变时的回调，可以
```kotlin
val valueProp by remember(30.0).property
val value by valueProp
SliderHorizontal(Modifier.height(20.scaled),0.0..100.0,valueProp.onSet { myAction() }) {
    TextFlatten { "value:$value".emit() }
}
```