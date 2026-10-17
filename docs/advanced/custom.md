---
title: Custom
nav_order: 1
parent: Advanced
---

# 自定义
手动处理事件、渲染，自由地制作DSL组件。

框架提供两个公开的基座：`Spacer()` 构造一个空白组件（`DslComponent`），`Scope()` 构造一个容器（`DslScope`）。用 `change` 在基座上叠加自己的实现即可。
```kotlin
context(ctx: DslContext)
fun MyComponent(
    modifier: Modifier = Modifier,
    color: Color,
    id: Any
) = Spacer(modifier, id).change {
    object: DslComponent by it {
        var prop by local { false }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton): Boolean {
            //...
            return it.mouseDown(mouseButton)
        }
    }
}
```
`Spacer(modifier, id)` 会创建组件并把它加入外部作用域（如`Row`，`Box`），`change` 则替换掉该组件的实现，槽位和身份都不变。在这里也可以使用`local`/`stable`/`static`等函数来存储状态。

如果想给一个**已有**组件加上行为，而不是自己创建组件，就不需要基座：
```kotlin
context(ctx: DslContext)
fun DslChild.myDecorator(
    color: Color
) = change {
    object: DslComponent by it {
        var prop by local { false }

        context(eventModifier: KeyModifiers, mouse: Position)
        override fun mouseDown(mouseButton: MouseButton): Boolean {
            //...
            return it.mouseDown(mouseButton)
        }
    }
}
```
向`change`传入一个`(DslComponent) -> DslComponent`，能`override`其中的函数，给一个已有组件加上装饰。

容器用`Scope()`，它的参数可以指定两个轴的空间分配方式（`Box`/`Row`/`Column` 就是常见的几种组合）：
```kotlin
context(ctx: DslContext)
fun MyColumn(
    modifier: Modifier = Modifier,
    id: Any,
    function: DslFunction
) = Scope(modifier, id, Aligner.close(Align.MID), Aligner.weightedStrictByMin, function).change {
    object: DslScope by it as DslScope {
        override val contentMinWidth get() = 100.scaled
    }
}
```

## 接口委托
`DslComponent`是一个接口，`Spacer` 返回的是一个`final`类实现（容器的 `Scope` 同理）。接口委托可以达到接近于继承`final`类的效果：`object: DslComponent by it` 把一个对象的接口函数转发到另一个对象上，无论被转发的对象是`final class`，匿名`object`，还是普通的`open class`，都能转发并`override`其中的接口函数。

这种模式的好处是委托并不侵入修改原对象，减少了逻辑上的混乱。缺点是委托是编译期产生，如果接口有修改，需要重新编译（建议执行`./gradlew clean`避免编译缓存造成的bug）。
## `this` `super` `instance` `delegate`的区别
`DslComponent`有`instance`这个成员。设计这个成员是因为每次委托都产生一个新对象，所以一级装饰无法获得后级装饰的结果。
```kotlin
context(eventModifier: KeyModifiers, mouse: Position)
override fun mouseDown(mouseButton: MouseButton): Boolean {
    if(delegate.focusable) {
        // ...
        return true
    }
    return delegate.mouseDown(mouseButton)
}
```
在下一级装饰中
```kotlin
override val focusable get() = true
```
如果不传`instance`，前一级装饰不知道组件的`focusable`已经被设为了`true`，造成逻辑错乱。

`instance`的值应该在`build()`前被正确设置，必须是已经加上全部装饰后的最终`DslComponent`对象。

`delegate`访问的是前一级装饰后的对象。这个名称不是固定的，比如前面的例子里是`it`。`this`访问的是当前装饰产生的对象。`super`访问的是接口，调用的是接口中的默认实现。`instance`是最终装饰完成的对象。

## 布局过程
`build()`，`layoutHorizontal()`，`layoutVertical()`，`render()`这四个函数会依次被调用。

构建一个组件时需要保证调用顺序，但是不需要一一对应。例如：`LazyColumn`在自身的`layoutVertical()`中才开始调用子组件的`build()`，因为此时才知道有哪些组件需要构建。

子控件需要实现`outerMinHeight`和`outerMinWidth`供外部读取，让父控件在layout时给它分配至少这么多空间（不一定遵循）。

先水平布局再竖直布局的原因是`TextAutoFold`需要先确定宽度，才知道高度。如果反过来调用，会导致`outerMinHeight`不能正确计算。

## 画矩形以外的形状
框架有两个绘制原语：四边形与三角形列表。矩形、贴图、九宫格都由四边形表达，**任意三角形与多边形不在其中**——仿射映射只能把矩形映成平行四边形：

```kotlin
context(backend: DslRootRenderer<RP>, renderParam: RP, mouse: Position)
override fun <RP> render() {
    // 凸多边形：一个顶点一种颜色，内部展开成三角扇
    backend.fillPolygon(points, colors)
    // 任意三角形表：每三个顶点一个三角形，`vertices`每顶点 2 个 float
    backend.renderTriangles(vertices, colors)
}
```

`fillPolygon`每次调用都会重建数组，适合偶尔绘制的图形；每帧都画的多边形应当自己复用`renderTriangles`的数组。
