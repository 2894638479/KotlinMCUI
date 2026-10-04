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
`Tooltip`图层由框架为每个`Screen`自动创建，不需要也无法手动调用。

只要给组件加上`.tooltip {}`，当鼠标悬停或键盘选中该组件时，其中的内容就会渲染出来，并自动调整位置到屏幕内。就像原版`minecraft`中那样。

### 为组件声明`tooltip`
```kotlin
Button(Modifier.size(50.scaled,20.scaled)) {
    
}.clickable {  }.tooltip {
    Column {
        TextFlatten { "Test Tooltip".emit(Color.BLUE,18.scaled, DslCharStyle().shadowed.italic.underlined) }
        ColorRect(Modifier.height(1.scaled).padding(3.scaled),Color(180,180,180)) {}
        TextAutoFold { repeat(5) { "this is example tooltip".emit() } }
    }.tooltipBackground()
}
```
`.tooltip{}`的内容只在该组件被悬停或选中时才会渲染。`.tooltipBackground()`会让一个组件具有原版`minecraft`的`tooltip`背景框。
