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

## 翻译
语言文件（`assets/<modid>/lang/<语言>.json`）是翻译的**唯一来源**，键就是点分路径：

```json
{
    "mymod.page.title": "我的页面",
    "mymod.page.count": "共 %s 项"
}
```
在 DSL 里用`translate`取值，查不到时原样显示键名：

```kotlin
TextFlatten { translate("mymod.page.title").emit() }
TextFlatten { translate("mymod.page.count", 3).emit() }
```
额外参数会先转成字符串再替换占位符。游戏在读取语言文件时已把`%d`、`%f`这类数字占位符统一改写成`%s`，所以实际只有`%s`一种占位符。

不在 DSL 上下文里（例如入口的元数据）用`translateGlobal`。

框架自己的键都以`kotlinmcui.`开头。