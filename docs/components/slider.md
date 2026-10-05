---
title: Slider
nav_order: 5
parent: Components
---

# 滑条
支持`Int`、`Double`和`Float`。支持键盘操作，让焦点位于滑条上（可以点一下，也可以用方向键或Tab键选中），然后按下enter，就能通过方向键调节值。

`Double`用`ClosedFloatingPointRange`，`Int`用`IntProgression`，两者都通过`Axis`指定方向。
```kotlin
val valueProp = local { 30.0 }
var value by valueProp
Slider(Modifier.height(20.scaled), Axis.Horizontal, 0.0..100.0, valueProp) {
    TextFlatten { "value:$value".emit() }
}
```
如果想要监听值改变时的回调，可以
```kotlin
val valueProp = local { 30.0 }
var value by valueProp
Slider(Modifier.height(20.scaled), Axis.Horizontal, 0.0..100.0, valueProp.onSet { myAction() }) {
    TextFlatten { "value:$value".emit() }
}
```

## 颜色通道
颜色也走同一条滑条，只是把"范围"换成"通道"：
```kotlin
val colorProp = local { Color(220,120,40) }
Slider(Modifier.width(120.scaled), Axis.Horizontal, ADouble, colorProp, id = "alpha") {
    TextFlatten { "a: ".emit(); colorProp.value.aInt.toString().emit() }
}
```
- `prop` 是 `math` 的 `ColorProp`：`RDouble`/`GDouble`/`BDouble`/`ADouble`（0-255 的通道）、`HDouble`/`SDouble`/`VDouble`（色相/饱和度/明度）。
- `color` 是被编辑的颜色属性，滑块的填充色就是它当前的颜色。
- 轨道上画的是该通道自己的颜色渐变——`HDouble` 是整圈彩虹，其它通道是两端颜色的渐变。
- 键盘方向键按该通道走一步，移动是整数精确的：不会因为浮点误差把其它通道也带偏。
- 通道自带名字与读数（`ColorProp.name`、`ColorProp.toString(color)`），直接写进自己的标签里即可。