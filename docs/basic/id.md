---
title: ID
nav_order: 6
parent: Basic
---

# ID
ID可以是任意类型。子组件的ID会继承父组件的ID并延长。
## 如何使用ID
### 向函数传入ID
最保险的方式是向每个ui函数都手动传入一个不同的ID（只需保证在同一个scope内不同）。
```kotlin
Row(id = Unit) {
    if(bool) Button(id = 1) {}
    listOf("a","b","c").forEach {
        TextFlatten(id = it) { it.emit() }
    }
    Column {
        Button(id = "a") {}
        Spacer(id = "b")
        Spacer(id = "c")
    }
    Column {}
}
```
但是维护这么多id是很繁琐的。你会发现代码写成下面这样，控件依然会获得不同的id：
```kotlin
Row {
    if(bool) Button {}
    listOf("a","b","c").forEachWithId {
        TextFlatten { it.emit() }
    }
    Column {
        Button {}
        Spacer {}
        Spacer {}
    }
    Column {}
}
```
### 为什么能简化
由`kotlin`在`JVM`上的实现决定。以上面代码为例：每次执行到`Column{...}`时，传入的`lambda`可以用`lambda::class`的`==`来判断是否在同一位置声明；对于`{}`，可以用`==`来判断它们是否在同一位置声明。以区别两个`Column`。

我没找到任何`kotlin`官方关于这个行为的保证，所以这是一个比较激进的缩简。

所以，at your own risk。我倾向更简单的写法。
### 传递ID
```kotlin
context(ctx: DslContext)
fun MyFunc(color:Color,id:Any) = Row(id = id) {
    // ...    
}

context(ctx: DslContext)
fun MyFunc(
    color:Color,
    id:Any? = null,
    lambda: context(DslPreventContext) ()->Unit
) = Box(id = id ?: lambda::class) {
    // ...    
}
```
对于有`lambda`参数的函数，把`lambda::class`作为默认ID，同时允许调用者传入自定义ID。`null`代表使用默认ID。最好不要给`lambda`参数设默认值。

对于没有`lambda`参数的函数，把`id:Any`放在最后，以便使用`MyFunc {}`传入`{}`作为ID。

## 为什么需要ID
ID用来区分不同的组件。关于为什么需要ID，看下面这个示例：
```kotlin
context(ctx: DslContext)
fun MyUIComponent(text:String) = run {
    var counter by remember(0)
    Button { TextFlatten { "$text$counter".emit() } }.clickable { counter++ }
}

// in a ui function
Row {
    if(showCounter1) MyUIComponent("counter1")
    MyUIComponent("counter2")
}
```
两个counter会变得无法区分。在读写`counter`时，会调用`remember`返回的对象的`getValue(thisRef:Any?, property: KProperty<*>)`和`setValue(thisRef: Any?,property: KProperty<*>,value:T)`。
每一次在代码中使用`by`委托属性，对应一个`KProperty`对象。通过比较`KProperty`，能区分这个属性在源码中的位置是否相同，但是没法区分同一个函数多次调用。