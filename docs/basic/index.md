---
title: Basic
nav_order: 3
---

# 基础
这一章展示ui的基础写法。如果想要详细阅读，可以直接查看每一个小节。如果不想详细阅读，下面给一些示例：
```kotlin
var i by remember(0)

Button(Modifier.size(Measure.AUTO_MIN,Measure.AUTO_MIN)) {
    TextFlatten(Modifier.padding(7.scaled)) { "counter: $i".emit() }
}.clickable { i++ }

DefaultBackground()
```
这会在正中间展示一个可点击的按钮，且上面有文字。点击会发出按钮音效，边框高亮，并且计数会+1。按钮的大小会随着文字长度而变化。


```kotlin
Column {
    val strings by remember(mutableSetOf("a","bb","ccc"))
    var chosen by "".remember
    Row(Modifier.height(50.scaled)) {
        TextFlatten { "buttons: ".emit() }
        strings.forEachWithId {
            val weight by autoAnimate(if(chosen == it) 2.0 else 1.0)
            Button(Modifier.weight(weight).padding(5.scaled)) {
                TextFlatten { it.emit(color = if(chosen == it) Color.GREEN else Color.RED) }
            }.clickable {
                chosen = it
            }
        }
    }
    var color by animatable(Color.RED)
    TextFlatten(Modifier.padding(5.scaled).weight(0.0)) {
        "the second item in column".emit(color = color)
    }.clickable {
        color = if(color == Color.RED) Color.BLUE else Color.RED
        if(strings.size < 5) strings += "added"
    }.highlightBox()
    Spacer(Modifier.weight(Double.MAX_VALUE)) {}
}
DefaultBackground()
```
按照行、列布局，并增加了一些动画效果，具体请自行运行查看

如果要查看更多组件的示例，可以浏览`TestPage()`相关源码。