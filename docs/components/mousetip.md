---
title: Mouse Tip
nav_order: 4
parent: Components
---
# 鼠标
跟随鼠标渲染。可以用`Modifier.align{}`来控制相对于鼠标的位置。
```kotlin
MouseTip {
    Column {
        Button {
            // ...
        }
        // ...
    }
}
```
可以正常在里面编写ui。

## `Tooltip`
```kotlin
Tooltip(id)
Tooltip {}
```
会把鼠标悬停/键盘选中的控件上的`.tooltip {}`中的内容渲染出来，并自动调整位置到屏幕内。就像原版`minecraft`中那样。

### 为组件声明`tooltip`
```kotlin
Tooltip {}

Button(Modifier.size(50.scaled,20.scaled)) {
    
}.clickable {  }.tooltip {
    Column {
        TextFlatten { "Test Tooltip".emit(Color.BLUE,18.scaled, DslCharStyle().shadowed.italic.underlined) }
        ColorRect(Modifier.height(1.scaled).padding(3.scaled),Color(180,180,180)) {}
        TextAutoFold { repeat(5) { "this is example tooltip".emit() } }
    }.tooltipBackground()
}
```
只有`Tooltip{}`存在时，组件的`.tooltip{}`内的ui才有机会出现。`.tooltipBackground()`会让一个组件具有原版`minecraft`的`tooltip`背景框。
