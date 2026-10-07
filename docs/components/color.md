---
title: Color
nav_order: 7
parent: Components
---

# 颜色控件
`widget` 包里有一套颜色编辑控件，从"单个通道的滑条"到"完整取色器"都有。它们只用公开 DSL 写成，可以与其它组件自由组合。

## 一个完整的取色器
```kotlin
val colorProp = local { Color(220,120,40) }
Colors.ColorEdit(colorProp, id = "editor")
```

从上到下依次是：预览色块与十六进制输入框；调色板、色相条与透明度；七个通道的竖向滑条（标着通道名的首字母 `r g b a h s v`），每条轨道上是该通道自己的颜色渐变。

- `picker` 参数选择初始调色板（`ColorPicker.Square` 或 `ColorPicker.Triangle`），旁边的按钮可以在两者间切换，选择按 `id` 记住。
- 三角模式依赖后端的 `renderTriangles`，它是必需的原语，所以这个模式总是可用。
- `id` 与其它组件一样是身份，默认取 `prop`；同一个 `prop` 在多处出现时要显式传不同的 `id`。

## 单独使用
| 控件 | 作用 |
| --- | --- |
| `Colors.ColorSquare(prop, modifier, id)` | 色相固定的饱和度/明度方块 |
| `Colors.ColorTriangle(prop, modifier, id)` | 顶角纯色相、左下白、右下黑的三角调色板；需要后端支持 |
| `Colors.ColorHue(prop, modifier, axis, id)` | 色相条，只改色相，饱和度和明度保持不动；`axis` 决定横竖 |
| `Colors.ColorHueTriangle(prop, modifier, id)` | 三角色相盘：三个角是纯红/绿/蓝，内部是它们的混合；需要后端支持 |
| `Colors.ColorText(prop, modifier, id)` | 十六进制输入框 |

单通道不改颜色，用核心滑条的彩色重载即可（见[滑条](slider)）：`prop` 是 `prop` 包里的 `ColorProp`（`RDouble`、`ADouble`、`HDouble` …），轨道上画该通道自己的颜色渐变。

```kotlin
Row {
    Colors.ColorSquare(prop, Modifier.size(80.scaled,80.scaled), "square")
    Slider(Modifier.width(60.scaled), Axis.Horizontal, ADouble, prop, id = "alpha") {
        TextFlatten { "a: ".emit(); prop.value.aInt.toString().emit() }
    }
}
```

## 交互与格式
- 在调色板里按下并拖动即取色，松手结束；小方框标记当前颜色所在的位置。拖动时指针可以离开调色板，此时会保持在最近的位置继续取色。
- 调色板自带一圈内边距：标记画在内容区之外，但始终留在组件自己的范围内，不会盖到相邻组件上。
- 十六进制接受 `#rgb`、`#rgba`、`#rrggbb`、`#rrggbbaa`（`#` 可省略、大小写随意）。颜色不透明时显示 6 位，带透明度时显示 8 位；输入过程中只要当前文本能解析就立即生效，不能解析时颜色保持不变。
- 点进输入框后它归你输入，失去焦点后才重新按颜色刷新。
- 通道的名字和读数由 `ColorProp` 自己提供（`name` 与 `toString(color)`）：`r`/`g`/`b`/`a` 是 0-255 的整数，`h` 是角度，`s`/`v` 是百分比。自己用核心滑条时，把这两句写进滑条的内容即可，不必再维护一套读数。
