---
title: 后端
nav_order: 9
parent: Dev
---

# 后端
核心库不认识任何 Minecraft 类型。所有平台能力都通过 `backend` 包的接口暴露，由配套的后端 mod 实现——这样核心库能独立编译测试，也能让同一个 jar 适配不同加载器与不同游戏版本。

这一页讲**契约**。官方实现（独立仓库）是怎么满足它的、踩了哪些平台限制，见 [Fabric 后端实现](backend-fabric)；后端的使用与构建见[后端](../backend/index)一章。

## 接口组合
`DslBackend` 自己**没有任何成员**，它只是把若干职责接口聚合成"一个后端"：

| 接口 | 职责 |
| --- | --- |
| `DslBackendRenderer<RP>` | 绘制原语、裁剪、变换、字体 |
| `DslBackendScreenFactory<SC>` | 创建 Screen |
| `DslBackendInput` | 鼠标位置、按键/鼠标状态查询、剪贴板、IME 候选区 |
| `DslBackendMetadata` | 版本、名称、本地化、打开链接一类元操作 |
| `DslBackendUtils` | 资源与本地图像加载等杂项 |

`DslBackendScreenHolder<SC>` **不属于** `DslBackend`，它是 `create()` 的返回值，负责持有平台侧 screen 对象并实现 `show()`。

抽象成员总数为三十余个，但**其中只有三个有默认实现**（纯色矩形、渐变矩形、贴图矩形）——它们可以用 `renderVertices` 表达，所以被实现成默认方法，减轻后端负担。

## 渲染原语与不变量
框架只要求后端实现**一个基础图元**：

```kotlin
fun renderVertices(vertices: List<Pair<Position, Color>>)
```

顶点数必须是 3 的倍数，每三个组成一个三角形。带纹理的重载额外接受一个 `ImageHolder` 与一一对应的 UV 列表。

由此带来几条必须遵守的契约：

- **坐标空间统一**：`Measure`、`Position`、`Rect`、`DslBackendInput.mouse`、根节点的 `rect` 处于同一空间，原点左上、Y 轴向下。`guiScale` 只用于 `.scaled` / `.unscaled` 的换算，不是绘制坐标的一部分。
- **UV 处于图像像素空间，且允许越界**。平铺（repeat）与九宫格策略依赖越界 UV 加纹理环绕模式，后端**不得**在这里做钳制。
- **`guiScale` 必须在运行期保持稳定**。组件持有的 `scale` 是建树那一刻的快照，若中途改变缩放，已建好的组件仍会按旧值计算。
- **`withScissor` 嵌套求交，`withTransform` 嵌套复合**。进入这两个作用域前必须先 `flush()`，否则已经排队的顶点会用错裁剪/变换。
- **`getFont(name)` 必须对同一名称返回同一实例**。文本行在布局期与渲染期分别取字体，并用引用比较判断"字体是否被换掉了"。
- **帧末必须有兜底 `flush()`**。内置装饰器都会在提交自己的绘制后自行 flush，但这是约定而非框架保证：后端自己排队的顶点仍需在帧末统一提交。

`TransformStack` 是给后端参考的变换栈实现（当前主代码与测试都没有引用它）；后端也可以自行用平台原生的变换/裁剪栈，只要满足上述语义。

## 生命周期
```kotlin
showScreen(title) { /* DSL */ }
// 等价于
backend.create(title) { /* DSL */ }.show()
```

`create()` 构造 `DslDataStore`，其中又构造 `DslScreen`。**`create()` 必须无副作用**：根节点会惰性地调用一次 `create("Dsl Overlay") { ... }` 来建立 Overlay 层，并且**只取其中的 `dslScreen`、从不 `show()`**。如果 `create()` 顺手做了注册、着色器初始化之类的可见副作用，就会在无人察觉的情况下被执行一次。

每帧后端要做三件事：

1. 把 `TopComponent.rect` 设为整屏尺寸；
2. 以 `(backend, renderParam, mouse)` 三个上下文接收者调用根节点的 `render<RP>()`；
3. 帧末 `flush()`。

`TopComponent.render()` 会把整屏矩形分发给 Overlay 层子节点，然后按逆序渲染。它内部会重算悬停状态，所以**输入与渲染不需要各自维护一套坐标判定**。

### 输入由后端采集
框架不采集输入。后端必须在平台回调里转发到根节点对应的方法：`keyDown` / `keyUp` / `mouseDown` / `mouseUp` / `mouseMove` / `mouseScrollVertical` / `mouseScrollHorizontal` / `charTyped` / `dropFiles` / `globalPreedit`。修饰键由后端从平台位掩码填充，框架内部不做推导。

根节点的 `mouseDown` 会先根据命中结果转移焦点，然后**仍然**把事件向下传递——点击既是焦点操作也是普通事件。

关闭的唯一出口是 `dataStore.onClose`。不要在这个回调里再调 `dslScreen.close()`，那会造成递归。

`dataStore.pauseGame` 默认为 `true`，是否使用由后端决定。

## 后端注册与入口
后端通过一个全局可变的 provider 注册自身：

```kotlin
var dslBackendProvider: (() -> DslBackend<*, *>)? = null
val dslBackend get() = dslBackendProvider?.invoke()
    ?: throw IllegalStateException("DSL failed to find a backend...")
```

注意 `dslBackend` **每次访问都会调用 provider**，所以 provider 必须返回稳定的单例，不能在每次调用时新建后端。

加载器侧的入口发现见 [入口](entry)。

## 线程与调度
UI 构建状态是线程受限的：`DslContext` 记录创建它的线程，所有上下文槽位读写都做线程校验。

由于**每帧都会新建一个 `DslContext`**，而组件对象会跨帧留存（它们持有创建时的上下文引用），实际约束是：`create` / `build` / `render` / 输入转发 / 关闭回调必须全部发生在同一个线程上。

`mainDispatcher` 供属性作用域创建协程使用（`clickable { launch { } }` 就跑在它上面），因此它必须是**主线程的串行调度器**，不能是并行调度器。

## 写一个最小后端
必须实现的成员分布大致是：渲染接口十余个（缩放、flush、两个 `renderVertices`、原版风格的按钮/槽位/容器/提示底纹/物品/默认背景、裁剪、变换、取字体）、输入接口四个、元数据五个、杂项若干个、以及 Screen 工厂一个。

最低限度可以这样：

1. 选一个渲染参数类型 `RP` 与 screen 类型 `SC`（测试后端两者都是 `Unit`）；
2. 用 `renderVertices` 实现默认的三个矩形方法就能画出基本界面；
3. 字体需要实现行高、字形度量、字形绘制；字形需要提供字宽、粗体偏移、阴影偏移；
4. `create()` 返回一个 holder，`show()` 里交给平台打开界面；
5. 帧循环里按上面的三件事驱动根节点。

测试用的假后端是这方面最可靠的样本：它把各接口分别委托给独立的小实现类，`create` 直接构造 `DslDataStore` 与 `DslScreen`，并提供一个"跑一帧"的辅助函数（设置鼠标、设置根矩形、渲染、帧后清理）。它也展示了如何用固定的字体度量（字宽与行高）让文本布局可断言。

## 未使用的接口成员
- `isMouseDown` 的裸索引入口没有调用点，`isInWorld`、`forceLoadLocalImage`、`openUri` 在主代码中也没有调用点；它们要么是给真实后端用的，要么需要按"接口最小"原则重新评估。
