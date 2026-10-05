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
Controls.ColorEdit(colorProp, id = "editor")
```

从上到下依次是：预览色块与十六进制输入框；调色板、色相条与透明度；七个通道的竖向滑条（`r g b a h s v`）。

- `picker` 参数选择初始调色板（`ColorPicker.Square` 或 `ColorPicker.Triangle`），旁边的按钮可以在两者间切换，选择按 `id` 记住。
- 后端不具备三角表能力（`DslBackendTriangles`）时不会提供三角模式——画不出来的取色板比没有更糟。
- `id` 与其它组件一样是身份，默认取 `prop`；同一个 `prop` 在多处出现时要显式传不同的 `id`。

## 单独使用
| 控件 | 作用 |
| --- | --- |
| `Controls.ColorSquare(prop, modifier, id)` | 色相固定的饱和度/明度方块 |
| `Controls.ColorTriangle(prop, modifier, id)` | 顶角纯色相、左下白、右下黑的三角调色板；需要后端支持 |
| `Controls.ColorHue(prop, modifier, axis, id)` | 色相条，只改色相，饱和度和明度保持不动；`axis` 决定横竖 |
| `Controls.ColorHueTriangle(prop, modifier, id)` | 三角色相盘：三个角是纯红/绿/蓝，内部是它们的混合；需要后端支持 |
| `Controls.ColorSlider(channel, prop, modifier, id)` | 单通道滑条（`ColorChannel`），右侧带该通道的读数 |
| `Controls.ColorText(prop, modifier, id)` | 十六进制输入框 |

```kotlin
Row {
    Controls.ColorSquare(prop, Modifier.size(80.scaled,80.scaled), "square")
    Controls.ColorSlider(ColorChannel.Alpha, prop, Modifier.width(60.scaled), "alpha")
}
```

## 交互与格式
- 在调色板里按下并拖动即取色，松手结束；小方框标记当前颜色所在的位置。拖动时指针可以离开调色板，此时会保持在最近的位置继续取色。
- 调色板自带一圈内边距：标记画在内容区之外，但始终留在组件自己的范围内，不会盖到相邻组件上。
- 十六进制接受 `#rgb`、`#rgba`、`#rrggbb`、`#rrggbbaa`（`#` 可省略、大小写随意）。颜色不透明时显示 6 位，带透明度时显示 8 位；输入过程中只要当前文本能解析就立即生效，不能解析时颜色保持不变。
- 点进输入框后它归你输入，失去焦点后才重新按颜色刷新。
- 通道滑条的读数按通道给单位：`r`/`g`/`b`/`a` 是 0-255，`h` 是角度，`s`/`v` 是百分比。
