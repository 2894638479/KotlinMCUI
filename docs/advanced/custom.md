---
title: Custom
nav_order: 1
parent: Advanced
---

# 自定义
手动处理事件、渲染，自由地制作DSL组件。

```kotlin
context(ctx: DslContext)
fun MyComponent(
    modifier: Modifier = Modifier,
    color: Color,
    id: Any
) = collect(
    object: DslComponent by DslComponentImpl(newChildId(id), modifier) {
        var prop by remember(false)

        context(instance: DslComponent)
        override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
            //...
        }
    }
)
```
向`collect()`传入一个`DslComponent`，就会把这个组件加入到外部的作用域中（如`Row`，`Box`）。在这里也可以使用`remember`等。
```kotlin
context(ctx: DslContext)
fun DslChild.myDecorator(
    modifier: Modifier = Modifier,
    color: Color,
    id: Any
) = change {
    object: DslComponent by it {
        var prop by remember(false)

        context(instance: DslComponent)
        override fun mouseDown(mouse: Position, mouseButton: MouseButton): Boolean {
            //...
        }
    }
}
```
向`change`传入一个`(DslComponent) -> DslComponent`，能`override`其中的函数，给一个已有组件加上装饰。

## 接口委托
`DslComponent`是一个接口，`DslComponentImpl`是一个`final`类。接口委托可以达到接近于继承`final`类的效果。
```kotlin
val component = object:DslComponent by DslComponentImpl(modifier,id) {}
```
接口委托把一个对象的接口函数转发到一个新对象上。无论原对象的类型是什么，包括`final class`，匿名`object`，还是普通的`open class`，都能转发并`override`其中的接口函数。

## `this` `super` `instance` `delegate`的区别
大部分成员函数都有`context(instance: DslComponent)`的参数。设计这个参数是因为每次委托都产生一个新对象，所以一级装饰无法获得后级装饰的结果。
```kotlin
override fun mouseDown(mouse:Position, mouseButton: MouseButton): Boolean {
    if(delegate.focusable) {
        // ...
        return true
    }
    return delegate.mouseDown(mouse,mouseButton)
}
```
在下一级装饰中
```kotlin
override val focusable get() = true
```
如果不传`instance`，前一级装饰不知道组件的`focusable`已经被设为了`true`，造成逻辑错乱。

`instance`由事件分发者传入，必须是已经加上全部装饰后的最终`DslComponent`对象。向下分发事件时要注意`it.run { mouseDown() }`来确保`instance`传入。

`delegate`访问的是前一级装饰后的对象。这个名称不是固定的，比如上一个例子里是`it`。`this`访问的是当前装饰产生的对象。`super`访问的是接口，调用的是接口中的默认实现。`instance`是最终装饰完成的对象。
