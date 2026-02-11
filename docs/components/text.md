---
title: Text
nav_order: 2
parent: Components
---

# 文字
```kotlin
TextFlatten {
    "a".emit(color = Color.RED)
    "b".emit(style = DslCharStyle().italic)
}
```
不要忘记`emit()`，否则这个字符串不会显示。可以为字符串的每个部分指定字号、样式、颜色。

`size`建议指定为`9.px`的整数倍。
## 换行
`TextFlatten`永远是一行，即使里面有`\n`字符也会自动去掉。如果要用`\n`换行应该使用`TextFoldable`。如果想让文字填满长度后自动换行，应该使用`TextAutoFold`。

它们会自动计算布局所需的最小空间，并“撑开”外部空间。`TextAutoFold`的最小宽度是0。