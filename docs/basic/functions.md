---
title: Functions
nav_order: 5
parent: Basic
---

# 函数
使用函数表示一个ui组件，或装饰。形式如下：
```kotlin
// ui function
context(ctx: DslContext)
fun MyFunc(a: A, b: B): DslChild

// decorator function
context(ctx: DslContext)
fun DslChild.decorate(c: C, d: D): DslChild

// usage
Row {
    MyFunc(a,b)
    MyFunc(a,b).decorate(c,d)
}
```
大小写和参数位置只是习惯上的约定，不做强制限制。

每个ui函数都有`DslContext`作为`context parameter`。`ctx`传递了ui函数执行所需的参数。

## 执行时机
每帧都会执行。但是对于立即模式绘制的游戏来说，不是明显的开销。相比之下带来的好处更多。

## 示例
展示一些内置ui函数的签名（不一定准确）
```kotlin
context(ctx: DslContext)
fun Box(
    modifier: Modifier = Modifier,
    id:Any? = null,
    function: DslFunction
): DslChild

context(ctx: DslContext)
fun Button(
    modifier: Modifier = Modifier,
    color: Color = Color.WHITE,
    id:Any? = null,
    function: DslFunction
):DslChild
```
装饰函数：
```kotlin
context(ctx: DslContext)
fun DslChild.animateHeight(
    duration: Duration = 0.5.seconds,
    interpolator: Interpolator = Interpolator.default
): DslChild

context(ctx: DslContext)
fun DslChild.clickable(
    enabled: Boolean = true,
    block: context(DslPreventContext, DslDataStoreContext) DslComponent.()->Unit
): DslChild
```

## 编写自己的函数

```kotlin
context(ctx: DslContext)
fun MyFunc(modifier: Modifier = Modifier, color: Color, text:String, id: Any) 
= Column(modifier, id = id) {
    ColorRect(color = color) {}
    TextFlatten { text.emit(color = color) }
}
```
函数内也允许使用`if else`、`when`、`for`、`forEach`等方式控制执行流，来控制一些组件是否被显示。

所有ui函数都传入了id参数。下一节讲id的作用。

