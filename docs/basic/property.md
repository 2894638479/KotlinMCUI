---
title: Property
nav_order: 7
parent: Basic
---

# 局部属性
```kotlin
var i by local { 0 }

Button {
    TextFlatten(Modifier.padding(7.scaled)) { "counter: $i".emit() }
}.clickable { i++ }
```
这段代码生成了一个按钮，每次点击它，显示的数值就会+1。

关于`kotlin`属性委托语法：[link](https://book.kotlincn.net/text/delegated-properties.html)

## 用法
以下写法如果没有被`idea`自动处理`import`，把鼠标点在`by`上并按`Alt+Enter`。
```kotlin
var a by local { 3 }
val b by local(customId) { "alice" }
var c by local.animatable { 20.0 }

val size by local.autoAnimate { if(expanded) 50.px else 25.px }
val data by stable { loadData() }
var count by static { 0 }
// ...
```
`local{}`是`local.property{}`的简写。`property{}`为普通变量，`animatable{}`和`autoAnimate{}`是动画变量，`cached(key){}`为缓存变量。

`local`,`stable`,`static`表示变量的生命周期。

## 指定id
一般情况下不需要指定id。使用前述的语法即可。

默认使用的追加id为传入lambda对象的`::class`。可以手动传入指定的id。

`local`和`stable`变量存在每个Screen单独管理的map中。存储key为当前context的id后追加上传入的id。正常情况下每个变量应该有一个固定且唯一的id。

`static`变量存储在一张全局共享的map中。使用的key与context的id无关。
## 与普通val的区别
```kotlin
var a = 0
var b by local { 0 }
// ...
    .clickable {
        a++
        b++
    }
```
在ui中会发现`a`的值始终是0，而`b`的值随着点击自增。

只要你想让这个值像一个稳定的变量，反复读写它的值，那就应该使用 `by remember()`



## 生命周期
### local
一段ui代码如果在某帧内没有被执行，其中的`local`属性会全部被销毁。下次执行到此处时会重新调用lambda的内容来创建这个变量。
### stable
`stable`属性的存活时间为首次创建到Screen销毁。延长了生命周期，可以使得用户切换页面再切回后依然保留之前的状态。这意味着其中的内存不会自动销毁，管理不当会造成泄露。如果不能确保不泄露，应优先使用`local`。
### static
`static`属性是全局共享的，效果类似全局变量或static变量。如果有需要用`static.property(id) { value }`的方式手动指定id，会使得具有相同id的声明指向同一属性（包括其它mod中的声明）。所以尽量指定不会重复的id，除非有意使用这种方式与其它mod通信。

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
val height by autoAnimate { if(bl) width else 30.px }
```
## 缓存
使用`cached`可以缓存一个计算开销较大的值。普通的加减乘除不建议使用。
```kotlin
val result by cached(key) {
    // compute...
}
```
对于计算耗时更长的值，建议使用协程调度到其它线程来执行，并缓存结果。
```kotlin
val deferred by local {
    async(Dispatchers.IO) {
        // ...
    }
}
if(deferred.isCompleted) {
    // ...
}
```
或者
```kotlin
var result by local<A?> { null }
local {
    launch(Dispatchers.IO) {
        // ...
        result = xxx
    }
}
```
对属性进行多线程读写是可以接受的，因为map读写在`local{}`中已经完成，后续过程中不进行map读写。
