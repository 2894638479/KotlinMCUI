---
title: Property
nav_order: 7
parent: Basic
---

# 局部属性
```kotlin
var i by remember(0)

Button {
    TextFlatten(Modifier.padding(7.scaled)) { "counter: $i".emit() }
}.clickable { i++ }
```
这段代码生成了一个按钮，每次点击它，显示的数值就会+1。

关于`kotlin`属性委托语法：[link](https://book.kotlincn.net/text/delegated-properties.html)

## 用法
以下写法如果没有被`idea`自动处理`import`，把鼠标点在`by`上并按`Alt+Enter`。
```kotlin
var a by remember(3)
val b by remember("alice")
var c by 20.scaled.remember
var d by remember {
    MyClass()
}

var expanded by remember(true)
val size by autoAnimate(if(expanded) 50.px else 25.px)

var weight by animatable(1.0)
// ...
```
建议给`remember()`和`animatable()`传入常数。如果要传入的对象构造耗时较长，可以使用`remember{}`，构造过程只会执行一次。
## 何时使用by
```kotlin
var a = 0
var b by remember(0)
// ...
    .clickable {
        a++
        b++
    }
```
在ui中会发现`a`的值始终是0，而`b`的值随着点击自增。

只要你想让这个值像一个稳定的变量，反复读写它的值，那就应该使用 `by remember()`
## 动画
`animatable`和`autoAnimate`能自动给可以插值的变量插值。它们都可以指定插值函数和时长。
### animatable
```kotlin
var height by animatable(10.px)
//...
    .clickable { height = 30.px }
```
点击后，读取`height`的值，会发现读到的值从`10`随着时间平滑变化到`30`。
### autoAnimate
和前者类似，但是只能委托给只读属性，用法略有不同。
```kotlin
val height by autoAnimate(if(bl) width else 30.px)
```

## 提取属性
```kotlin
val widthProp by 0.remember.property
SliderHorizontal(Modifier,0..100,widthProp) {}

val stringProp by "".remember.property
EditableText(Modifier,stringProp)
```
在前面的任何委托后面加上`.property`，可以得到`StableRW/StableRO`类型的属性。它可以用来传给`Slider`和`EditableText`等控件。

```kotlin
val width by widthProp
```
再委托一次，就可以向之前那样访问它的值。或者直接访问`widthProp.value`。

## 更多
`LocalRW`和`StableRW`的区别：
```kotlin
val aProp :LocalRW = remember(0)
var a1 by aProp
var a2 by aProp

val bProp : StableRW = run {
    val prop by remember(0).property
    prop
}
var b1 by bProp
var b2 by bProp
```
`a1`和`a2`访问的是不同的属性。因为`LocalRW`保存了当前上下文的`ID`，并把它和委托处的`KProperty`组合后作为键。`StableRW`在产生时就固定了键，所以能直接访问它的`.value`，也更适合作为参数传递。

这些属性都可以跨`Screen`访问。甚至`Screen`关闭后也能访问。。
