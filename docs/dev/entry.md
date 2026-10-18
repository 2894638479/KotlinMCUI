---
title: 入口
nav_order: 10
parent: Dev
---

# 入口
核心库需要一种方式发现"谁想往 GUI 列表里加一个界面""谁想常驻一层 Overlay""谁想在平台就绪后开一个窗口"，而它自己又必须能在客户端、服务端、以及没有游戏的环境里加载。入口机制就是回答这个问题的。

## 一个入口接口，两个种类
每个入口都实现 `DslEntryService`，提供三项身份信息：`name`（显示名）、`id`（稳定标识）、`icon`（图标引用），以及两个阶段的成员：

| 接口 | 作用 | 成员 |
| --- | --- | --- |
| `DslEntryService` | 每个入口都必须实现：身份 + 两个阶段 | `name` / `id` / `icon`、`initialize()`、`launch()` |
| `DslEntryGui` | 种类：可以作为 GUI 页面打开 | `context(DslContext) fun gui()` |
| `DslEntryOverlay` | 种类：可以作为叠加层绘制 | `context(DslContext) fun overlay()` |

一个类可以同时实现两个种类接口，因此"既是 GUI 页面又是叠加层"是常见组合；也可以两个都不实现——**只实现 `DslEntryService` 是合法的**，那是一个"没有界面、只做事"的入口（例如在 `launch()` 里打开一个窗口）。

种类由**实现了哪个接口**决定，不靠注册文件声明：判断"这个入口有没有 GUI"因此不需要实例化（`isAssignableFrom` 即可），也不会出现"列在文件里说自己有 GUI、其实没实现"这种矛盾。

`gui()` 与 `overlay()` 都是 `context(DslContext)` 函数——它们是 DSL 片段，不是普通回调。Overlay 的内容会被包进一个 `Box` 再渲染。

## 两个阶段
- `initialize()`：**装载期**。元数据可读，游戏客户端不可用，所以这一阶段只读取事实、注册内容；不得开窗、起服务或建根。
- `launch()`：**平台已发布、能建屏之后**。开窗、起服务、建根都属于这里。

`launch()` 由 `DslRuntime.launch()` 显式触发，**不是 `start()` 的副作用**："这个进程有平台"和"游戏已经就绪"是两件事——测试与 standalone 工具会发布平台，但那时并没有游戏可启动。

阶段只有两个。若将来出现第三段的诉求，先论证它是否真的需要独立时机，而不是惯性再加一个。

## 哪些入口会被加载
由注册文件里每个入口声明的端决定，并且**在加载类之前过滤**：

| 端 | 含义 |
| --- | --- |
| `client` | 进程里有游戏客户端 |
| `server` | 进程里有游戏服务端 |
| `standalone` | 没有任何游戏端（独立窗口、独立服务、测试工具） |

三者互斥。声明 `server` 的入口在客户端进程里**连类都不会被加载**，因此可以安全地在类体里引用服务端专有的类型。

**边界**：声明只决定"在哪些环境加载"。同一个类里混写两端专有的代码，在它被加载的那一端仍然会炸——**声明是加载许可，不是代码隔离**。

框架是运行期注册表，做不到编译期的依赖检查。真正的编译期隔离要靠使用方自己的源集划分（Fabric 的 `splitEnvironmentSourceSets`、Forge/NeoForge 的独立 client 包 + `DistExecutor`）；没有源集划分时，这一层就只剩"声明 + 运行期报错"。

集成服务器（单人游戏）**不在这条轴上**：它跑在客户端进程内，`Environment` 描述的是进程形态，不是"此刻在跑哪一侧的逻辑"。

## 注册方式
`META-INF/kotlinmcui/entries`，**一个文件**（多个 jar 的声明按 classpath 合并），每行一条：

```
# <全限定类名> <环境>[,<环境>...]
com.example.MyPage        client,server,standalone
com.example.ClientPanel   client
com.example.WindowEntry   client,standalone
```

- 环境必须**手写**：`client` / `server` / `standalone`，没有 `*`、没有别名；
- 空行与 `#` 开头的行跳过；其余每行必须**恰好两列**；
- **缺环境列、未知端名、同一类重复声明**（同文件或跨 jar）都会抛错——注册文件写错是构建错误，不该被跳过。

### 为什么不用 `java.util.ServiceLoader`
理由是**时机**：我们需要在**加载类之前**按端过滤，否则"声明为 `server` 的类在客户端不会被加载"这条保证根本不成立；而 `ServiceLoader` 只交给你已经构造好的实例。其次它的文件格式由 JDK 固定（一行一个类名），放不下"环境"这第二列。

自建加载器同时支持两种写法：公开无参构造器可直接构造，否则取 `INSTANCE` 静态字段——后者才是 Kotlin `object` 的常规路径，不当作"回退"来告警。构造器存在但**抛异常**时也不会被 `INSTANCE` 悄悄顶替：那会把失败藏在一个没人要的第二个实例后面，正确做法是让这一个入口出局并带上异常打日志。

### 框架自带的示例入口
核心 jar 自己的注册表声明了两条示例入口（`TestEntry$TestPage` 与 `TestEntry$DebugOverlay`，环境为 `client,server,standalone`），因此它们**在生产构建里同样会被加载**。

这是有意的：入口页开箱就有内容可点，后端也拿它们当参考内容做截图验收；`DebugOverlay` 是 Overlay，默认不启用。

代价也要写明白：它们随核心发布，**使用方无法移除**，只会在入口页里多出两条不是自己写的记录。如果将来这变得碍事，正确做法是把示例搬到独立产物或改成仅在开发环境声明——而不是在心里记住"那两条可以忽略"。

## 加载顺序
```kotlin
DslRuntime.install { Metadata }   // 内部调用 DslEntryLoader.discover(environment)
```

`discover` 的动作顺序是**刻意排的**，每一步都只在前一步通过后才发生：

1. 读注册文件（多 jar 合并）并解析校验；
2. **按 `metadata.environment` 过滤**——不匹配的条目到这一步就没有了；
3. `Class.forName(name, initialize = false)` 只解析类、不初始化；
4. 用 `isAssignableFrom` 判定 `isGui` / `isOverlay`（**不实例化**）；
5. 最后才实例化，并调用所有通过过滤的入口的 `initialize()`。

`launch()` 阶段同样只作用于这份过滤后的快照：先校验平台已发布（否则抛错），再幂等地调用每个入口的 `launch()`。第二次调用只打一条警告并返回。

`discover` 不由后端直接调用：它由 `DslRuntime.install` 在启动时调用一次，`install` 自己保证单次。

只有这两个阶段会被**调用**；GUI 与 Overlay 的内容只在用户打开列表或 Overlay 被启用时才执行。

`DslEntryLoader.entries` 返回的是登记结果的**快照**，调用方（列表页每帧遍历它）可以安全持有与遍历，不会因为重新 `discover` 而看到半截状态。

### 加载不出来的条目
注册文件里列了、但用不了的条目不会静默消失。四种情形——类找不到、没实现 `DslEntryService`、构造不出来（没有可用构造器 / 没有 `INSTANCE` / 构造器或静态初始化块抛错）、以及**两个 Overlay 声明了同一个 id**（先声明者胜，后者的整条记录进失败列表）——都会在 `brokenEntries` 里留下一条 `Failure`：

```kotlin
class Failure(val className: String, val hookName: String, val reason: String)
```

`reason` 是给页面看的一行话（`class not found`、`does not implement DslEntryService`、`could not be initialised (missing dependency?)`、`overlay id 'x' is already used by …` 之类），完整堆栈在日志里。记录按"类名 + 种类"去重：同一个类在两个地方出问题就是两条记录。

配置页把这些记录**追加在列表末尾**，每条一行矮行：没有图标、没有开关、也不受"显示代码入口"开关影响——一个加载失败的条目正是最该被看见的东西。

这一块解决了一个真实痛点：Overlay 的启用状态是按 **id** 存在 `overlays.json` 里的，如果那个 Overlay 的入口坏了，以前会静默什么都不画；现在能在列表里看到"你启用的那个入口坏了，原因是什么"。

### 缓存与标志
实例按类缓存，同一个类无论通过几个接口被发现都只实例化一次。缓存项附带一组标志：

| 标志 | 含义 |
| --- | --- |
| `environments` | 注册文件里声明的端（原样显示，不推导） |
| `isGui` / `isOverlay` | 该入口是否实现了对应的种类接口 |

GUI 列表页就是靠这组标志给每个入口标注徽章（端与种类），并据此决定点击是否打开界面、是否显示 Overlay 开关。同一套标志也用于在开发时确认"某个入口是否真的在当前环境被加载和调用"。

## Overlay 的启用
Overlay 默认**不启用**。启用状态是一个**有序 id 列表**：只有列在其中的 Overlay 才会被渲染，列表顺序即层叠顺序（越靠前越上层）。GUI 列表页的开关与"置顶"分别调用 `setOverlayEnabled` / `bringOverlayToTop`，两者都会立刻写回配置。

配置存放在 `<configDir>/kotlinmcui/overlays.json`，内容就是 Overlay 的 `id` 有序数组（模型见 `OverlayConfig`），由[配置工具](config)读写。读取发生在 `DslRuntime.install` 期间——那时元数据（配置目录）已经就绪，而后端本体还不需要被构造。三点设计意图：

- 用 **id** 而不是实例或列表下标做标识：下标会随登记顺序变，实例每次启动都会重建，只有 id 在两次启动之间稳定；
- **未安装模组的 id 保留在文件里**：临时卸掉一个模组不会丢掉它的开关状态，装回来仍然按原来的顺序渲染；
- 两个 Overlay 共用同一个 id 时无法区分，因此**先声明者胜**，后者不注册并作为失败条目显示出来（同上节）。

根节点的 Overlay 层每帧执行：

```kotlin
enabledOverlays.forEachWithId { Box { it.overlay() } }
```

`enabledOverlays` 是"当前已登记的 Overlay 按启用顺序解析"的结果快照，因此文件里存在但当前没安装的 id 不会出现在这里。两点设计意图：

- 用 `forEachWithId` 让每个 Overlay 的身份由 Overlay 对象本身决定，而不是由书写位置决定——这样调整列表顺序不会让某个 Overlay 的状态串到别的 Overlay 上；
- 每个 Overlay 各自包一层 `Box`，使它们成为互相独立的层叠容器，一个 Overlay 内部的布局不会影响另一个。

这条路径是在根节点的 `DslScreen` 里执行的，而那个 Screen 只被建立、从不 `show()`——它是常驻的 Overlay 承载层，不是普通界面。
