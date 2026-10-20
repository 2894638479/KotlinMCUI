---
title: 后端
nav_order: 9
parent: Dev
---

# 后端
核心库不认识任何 Minecraft 类型。所有平台能力都通过 `backend` 包的接口暴露，由配套的后端 mod 实现——这样核心库能独立编译测试，也能让同一个 jar 适配不同加载器与不同游戏版本。

这一页讲**契约**。官方实现（独立仓库）是怎么满足它的、踩了哪些平台限制，见 [Fabric 后端实现](backend-fabric)；后端的使用与构建见[后端](../backend/index)一章。

## 接口组合
没有"后端对象"这个东西了。后端按职责分别实现下面几个接口，框架只认这些接口，不认"一个实现了全部的后端"：

两组的边界有一条判据：**元数据是"进程启动时就可读、且必须无副作用的事实"，工具是"需要一个活着的平台才能执行的操作"**。名称、本地化与打开链接都要求平台已经起来，所以它们属于工具而不属于元数据。

| 接口 | 职责 | 归属 |
| --- | --- | --- |
| `DslPlatformMetadata` | 目录、游戏版本、加载器、运行环境 | 进程级 |
| `DslPlatformUtils` | 本地化、退出、本地图像加载与强制重载、世界判定 | 进程级 |
| `DslRootUtils` | 剪贴板、朗读、按钮音效、打开链接、截图、会话调度器 | 每个根 |
| `DslRootRenderer<RP>` | 缩放、提交、绘制原语、裁剪、变换、取字体 | 每个根 |
| `DslRootInput` | 按键/鼠标状态查询、鼠标位置、IME 候选区 | 每个根 |
| `DslRootScreenFactory<SC>` | 创建 Screen | 每个根 |

一个对象可以同时实现多个接口（例如 `DslPlatformUtils` 与 `DslRootUtils` 用同一个实现），实现者不因"分了接口"就必须拆成多个对象。

`DslScreenHolder<SC>` **不属于**上面任何一个，它是 `create()` 的返回值，负责持有平台侧 screen 对象并实现 `show()`。

抽象成员总数为三十余个：渲染接口里的绘制成员是 `renderQuad` 与 `renderTriangles` **两个**，纯色矩形、渐变矩形、贴图矩形都是 `renderQuad` 的**扩展函数**（不额外分配）。它们是扩展而不是接口成员，因此没法被覆写——后端想要平台自己的快速路径（例如原生的实心矩形填充），就在 `renderQuad` 里判断"无纹理且四角同色"，这样所有调用方一起受益。

## 启动顺序：`DslRuntime`
进程级只有元数据与一组平台工具，分两段，框架为这两段各提供一个入口，模组与后端都不再自己拼接顺序：

```kotlin
DslRuntime.install { Metadata }            // 模组初始化：平台事实 + 入口发现
// …客户端构造完成、可以建界面之后…
DslRuntime.start { DefaultUtils }          // 先到先得，返回 false 表示这次没生效
```

- `install` 立刻读一次元数据，并完成**入口发现**与框架自己的**配置读取**——这些只需要元数据。
- `start` 交出 `DslPlatformUtils`，只在第一次生效。第二个后端会被拒绝并记一条日志而**不抛错**：同一个游戏实例只应该装一个后端，而"装了两个"是用户的环境问题，不该升级成客户端崩溃。被拒的那一方从返回值和日志知道自己没有生效，**但照常建自己的根、照常服务**——是否自动驱动主循环由它自己决定，框架不代管。
- 两个 provider 变量（元数据、工具）是 `internal`，后端（独立模块）只能走这两个入口。两者都在锁内执行：单次语义必须在任何线程上都成立。
- 只依赖元数据的路径（配置读写、版本与环境查询）永远走 `dslMetadata`；它只来自 `install`，没有"回退去构造后端"这条路。
- 运行时的状态只有三件事：`installed`、元数据 provider、工具 provider。**没有任何根**——根属于建它的后端。

两条契约：

| 契约 | 理由 |
| --- | --- |
| 元数据实现不得有副作用 | 它在模组初始化阶段就会被读，那时窗口、渲染器、语言数据都还不存在 |
| 建根与建界面只能在 `start` 之后 | 平台发布之前，界面所需的渲染器、输入、调度器都还不存在；提前建根得到的不是"稍后能用的根"，而是根本不该存在的对象 |

## 根：由后端建、由后端驱动
框架不发根。后端在 `start` 之后自己构造：

```kotlin
val root = DslRoot(input, renderer, screenFactory, rootUtils)
```

- `renderer` 可空：为空表示这个根不绘制（连字体也没有）。需要绘制的组件会在使用处响亮失败，`scale` 退回 `1.0` 只影响布局近似。
- **根不负责渲染**。每帧后端写 `root.rect`，然后在 `context(renderer, 参数, root.input.mouse)` 里调用 `root.component.render()`。顺序是契约：渲染期会把 `rect` 复制到每个已挂载的屏。
- 挂载由 `root.children.collect(screen)` 完成，并且**每次 collect 都会把 `root.state` 盖到该屏的 store 上**；`remove` 不清章（隐藏不是销毁）。
- 同一进程里有多个后端时，各建各的根，互不可见——框架里没有"进程根"这个概念。
- **拆除门**：`root.close()`（幂等）让"继续用这个根"变成响亮失败——挂载、卸载、`component.render()`、`setFocus` 都会抛，晚到的一帧因此不会画进一个没人看的根。它**不**负责停你的定时器/执行器/录制器：顺序是**先 `close()`，再停你自己的驱动**。`root.isClosed` 报告状态。

## 预览
预览是工具，不是进程事实：`DslPreview.start(root)`，**显式调用**，画进你给的那个根。平台发布不会再顺带触发它。

## 渲染原语与不变量
框架要求后端实现**两个绘制成员**：下面的 `renderQuad` 与任意多边形的 `renderTriangles`。矩形是绝大多数内容的形状，多边形只有圆、圆角一类需要逼近时才用。

```kotlin
fun renderQuad(
    rect: Rect, lt: Color, rt: Color, lb: Color, rb: Color,
    image: ImageHolder? = null, uv: Rect? = null,
)
```

四个角依次是左上、右上、左下、右下，画成 `(lt, rt, lb)` 与 `(rt, rb, lb)` 两个三角形；省略 `image`/`uv` 就是纯色或渐变填充，带上则是贴图（`uv` 给出图像像素空间里的源矩形，九宫格与平铺靠越界 UV 加环绕模式表达）。

**这个原语必须保持零分配，这是它的设计约束而不是巧合。** 参数全是 value class 或已有引用，所以框架侧的三种矩形方法自身不产生垃圾。它原本的形态是"传一个顶点列表 `List<Pair<Position, Color>>`"：`Position` 是普通类、`Color` 与 `Measure` 是 value class，装进泛型集合就会逐个装箱，实测每次纯色矩形分配 **648 B**、每次贴图矩形 **1056 B**（约 25 个对象）。九宫格一次绘制要发 9 个贴图矩形、可编辑文本的选区是**每个选中字符一个矩形**，这些调用点会把单次成本成倍放大。因此不要再引入用集合表达顶点的图元。

任意多边形走同一个接口的第二个成员 `renderTriangles`（见[渲染](render)）：它是必实现的，组件直接调用，没有"取不到就不画"的分支。实现它时要注意延迟绘制必须把调用方的数组拷一份——`renderQuad` 的顶点数固定，可以放进字段，而三角形个数不固定。

由此带来几条必须遵守的契约：

- **坐标空间统一**：`Measure`、`Position`、`Rect`、`DslRootInput.mouse`、根节点的 `rect` 处于同一空间，原点左上、Y 轴向下。`guiScale` 只用于 `.scaled` / `.unscaled` 的换算，不是绘制坐标的一部分。
- **UV 处于图像像素空间，且允许越界**。平铺（repeat）与九宫格策略依赖越界 UV 加纹理环绕模式，后端**不得**在这里做钳制。
- **`guiScale` 必须在运行期保持稳定**。组件持有的 `scale` 是建树那一刻的快照，若中途改变缩放，已建好的组件仍会按旧值计算。
- **`withScissor` 嵌套求交，`withTransform` 嵌套复合**。裁剪是**提交期**的栅格状态：后端进入/退出裁剪作用域前必须把自己排队的顶点先交出去，否则那些顶点会被错误的区域裁掉。变换不是提交期状态——顶点在绘制时就已经烤进了矩阵。
- **HUD 与覆盖层必须等平台就绪后再画**。平台在首次加载完成前，它自己的管线/shader 缓存可能还没建好；这时提交一次绘制会让平台编出一个无效管线并缓存整个进程——OpenGL 常常宽容，Vulkan 会直接崩。判断"就绪"是平台知识（Fabric 上是 `Minecraft.isGameLoadFinished()`），所以这一条由后端在自己的每帧入口把关。
- **`getFont(name)` 必须对同一名称返回同一实例**。文本行在布局期与渲染期分别取字体，并用引用比较判断"字体是否被换掉了"。
- **帧末必须有兜底提交**。框架不再提供 `flush()`：绘制只保证"按调用顺序发起"，什么时候真正提交是后端的事；但一帧结束前，之前排队的顶点必须全部出去。
- **顺序契约是唯一的硬要求**：每个绘制按调用顺序生效。后端可以批处理、延迟、甚至重排，只要保证"相交的保持先后、不相交可任意换位"——不相交的两笔交换顺序不改变任何像素，这是可证明的。mc 后端因此改成**先记录再重放**：每笔绘制记下屏幕空间顶点与包围盒，用重叠偏序求"批次最少"的顺序后重放。实测 `TestLayoutPage`：提交 41→5、类型交替 38→3、UI 帧耗时中位数 6186→3058 µs，画面逐像素一致；排序后默认开启断言，用**从顶点重算的包围盒**（而非排序器缓存的盒）独立复核，任何相交对被反序就抛异常。`-Dkotlinmcui.order.reorder=false` 可关掉重排回到即时绘制。


变换与裁剪栈由后端自行用平台原生能力实现，只要满足上述语义。

## 生命周期
```kotlin
showScreen(title) { /* DSL */ }
// 等价于
root.screenFactory.create(title) { /* DSL */ }.show()
```

`create()` 构造 `DslDataStore(utils, root, …)`，其中又构造 `DslScreen`。**`create()` 必须无副作用**：根会惰性地调用一次 `create("Dsl Overlay") { ... }` 来建立 Overlay 层，并且**只取其中的 `dslScreen`、从不 `show()`**。如果 `create()` 顺手做了注册、着色器初始化之类的可见副作用，就会在无人察觉的情况下被执行一次。

每帧后端要做两件事：

1. 把 `root.rect` 设为整屏尺寸；
2. 在 `context(renderer, renderParam, root.input.mouse)` 里调用 `root.component.render()`，并在帧末把自己排队的顶点提交出去。

`root.component.render()` 会把整屏矩形分发给 Overlay 层子节点，然后按逆序渲染。它内部会重算悬停状态，所以**输入与渲染不需要各自维护一套坐标判定**。

### 输入由后端采集
框架不采集输入。后端必须在平台回调里转发到根节点对应的方法：`keyDown` / `keyUp` / `mouseDown` / `mouseUp` / `mouseMove` / `mouseScrollVertical` / `mouseScrollHorizontal` / `charTyped` / `dropFiles` / `globalPreedit`。修饰键由后端从平台位掩码填充，框架内部不做推导。

根节点的 `mouseDown` 会先根据命中结果转移焦点，然后**仍然**把事件向下传递——点击既是焦点操作也是普通事件。

关闭的唯一出口是 `dataStore.onClose`。不要在这个回调里再调 `dslScreen.close()`，那会造成递归。

`dataStore.pauseGame` 默认为 `true`，是否使用由后端决定。

## 后端注册与入口
后端通过 `DslRuntime.start` 交出平台工具，之后读 `dslUtils` / `dslMetadata`：

```kotlin
DslRuntime.install { Metadata }
DslRuntime.start { DefaultUtils }
```

两个 provider 是 `internal` 的 `var`，且带 `@Volatile`：写方在 `DslRuntime` 的锁内，读方（`dslUtils`、`dslMetadata`）不取锁，没有 volatile 就没有 happens-before 边，读者可以合法地一直读到 `null`。**这不是可选的优化，是发布语义的一部分。**

读口 `dslUtils` 每次访问都会调一次 provider，所以 provider 必须返回稳定的单例。

加载器侧的入口发现见 [入口](entry)。

## 线程与调度
UI 构建状态是线程受限的：`DslContext` 记录创建它的线程，所有上下文槽位读写都做线程校验。

由于**每帧都会新建一个 `DslContext`**，而组件对象会跨帧留存（它们持有创建时的上下文引用），实际约束是：`create` / `build` / `render` / 输入转发 / 关闭回调必须全部发生在同一个线程上。

调度器属于根：`DslRootUtils.sessionDispatcher` 是**会话级**的主线程调度器，供属性作用域创建协程使用（`clickable { launch { } }` 就跑在它上面），预览会话也把建屏与截图派发到它。它必须是串行调度器，不能是并行池：UI 构建状态是线程受限的。

根还持有 **`DslRoot.sessionScope`**：`SupervisorJob + sessionDispatcher`，**随 `root.close()` 取消**。要挂并发的协程用它，这样它不会活得比它操作的树更久。

**写 `local` / `stable` 只允许发生在该根的渲染线程上；从别处改，走 `DslRoot.post`。** 那两个存储是普通映射、不是并发容器，跨线程写就是数据竞态——`post` 只是把块交给根线程，并不把错误的写法变安全。`DslRootUtils.isOnSessionThread` 回答"我是不是已经在那个线程上"，`post(block)` 已经在则**内联执行**（否则会等自己），否则投到 `sessionScope`；两条路径都**不吞异常**。跨线程投递除调度器队列外**没有顺序保证**，不要拿它和帧循环排顺序。`post` 与 `sessionScope` 都不是接口成员——接口只有后端必须提供的两个事实。

## 写一个最小后端
必须实现的成员分布大致是：渲染接口十余个（缩放、`renderQuad`、`renderTriangles`、原版风格的按钮/槽位/容器/提示底纹/物品/默认背景、裁剪、变换、取字体）、输入接口四个、元数据五个、平台工具四五个、根工具七八个、以及 Screen 工厂一个。

最低限度可以这样：

1. 选一个渲染参数类型 `RP` 与 screen 类型 `SC`（测试后端两者都是 `Unit`）；
2. 实现 `renderQuad` 一个方法，三种矩形方法就都能用了，界面即可画出来；
3. 字体需要实现行高、字形度量、字形绘制；字形需要提供字宽、粗体偏移、阴影偏移；
4. `create()` 返回一个 holder，`show()` 里交给平台打开界面；
5. 帧循环里按上面的三件事驱动根节点。

测试用的假后端是这方面最可靠的样本：它把各接口分别委托给独立的小实现类，`create` 直接构造 `DslDataStore` 与 `DslScreen`，并提供一个"跑一帧"的辅助函数（设置鼠标、设置根矩形、渲染、帧后清理）。它也展示了如何用固定的字体度量（字宽与行高）让文本布局可断言。

## 未使用的接口成员
"框架没有调用"与"没有消费者"是两件事，这条清单按后者才成立：

- `isMouseDown` 的裸索引入口**不是死成员**：`isMouseDown(button: MouseButton)` 的默认实现就委托给它，而枚举重载被框架每帧调用做拖动对账（见[颜色](color)）。
- `reloadLocalImage` **不是死成员**：它是公开的用户 API，`docs/components/image.md` 明确教用户在本地文件变化后刷新缓存。DSL 层有一个同名的、要求 `DslExecuteContext` 的包装，就是给用户的事件处理器用的入口；平台层的这个方法没有框架内部调用点，但框架不调用不等于没有消费者。
- `localImage` / `remoteImage` / `translate` 在**帧线程**上调用，**不允许等待 io**（读文件、解码、开 socket、等待被这类工作持有的锁）。非 suspend 的函数本来就隐含这个承诺，而 window / web 两个实现都做成了同步 `ImageIO.read`——所以这条写进了接口注释。正确做法是先把工作丢给别的 dispatcher，这一帧返回 `empty`。
- `remoteImage` / `reloadRemoteImage` **没有默认实现**：没有 http 能力的后端也要给出实现——返回 `empty` / 空操作即可（页面照常渲染，网络图片不显示），**不要抛**。加一个默认实现会让"所有后端都必须有 http"或"所有后端都可以不管网络图片"这两种隐含契约二选一，所以交给实现方决定。
- **`start` 之前框架用 `UnpublishedUtils` 回答**：`translate` 返回 `null`（调用方回退到 key）、`isInWorld` 为 `false`、图片一律 `empty`、两个 reload 都是空操作、`quit` 直接退出进程。**全部良性，没有任何会抛异常的成员**；调度器不在这里——它属于根（`DslRootUtils.sessionDispatcher`），所以入口的 `initialize()` 可以安全地翻译文本、读元数据。
- **图片缓存淘汰是可选项**：后端可以永不淘汰。若要做，一种可行做法是——命中即刷新 `lastUsed`、每帧 `sweep`、按墙钟 TTL 淘汰、淘汰时释放纹理（mc 的 `TextureManager.release`），否则 `reloadLocalImage` 会不断留下 `minecraft:dynamic/dslimageid_N` 这类永不释放的纹理。这是**建议**，不是接口约束。
- `isInWorld` **确无消费者**：跨四个仓库的 `src` 都没有调用点，也没有任何文档使用它。而且它与下面的约定重复——"是否就绪"是平台知识、由后端在自己的每帧入口把关，框架再暴露一个世界判定没有额外信息。
- `openUri` **确无消费者**：同样跨四仓零调用，文档里也没有会出现链接的组件。要用它，得先有一个真的会打开链接的组件。

后两条是删除候选，前两条应当留下。改动接口前请以这份清单为准，不要按"框架没调"一刀切。
