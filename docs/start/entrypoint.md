---
title: Entrypoint
nav_order: 4
parent: Start
---
# 入口点
`kotlinmcui`提供一个加载器无关的入口点，并且二进制兼容。意味着你可以不再依赖`fabric/forge/neoforge`等加载器提供的入口点。

## 用法
### 入口点类编写
每个入口都实现`DslEntryService`；想提供界面就再实现`DslEntryGui`或`DslEntryOverlay`。可以同时实现多个。

更推荐使用`object`而不是`class`，虽然二者都能处理。

所有入口都必须实现`name`、`id`、`icon`三个成员（没有图标时`icon`写`ImageHolder.empty`），以及可选的`initialize()`与`launch()`。

```kotlin
object Entry : DslEntryService, DslEntryGui {
    override val name = "Example"
    override val id = "example"
    override val icon = ImageHolder.empty

    override fun initialize() {}

    context(ctx: DslContext)
    override fun gui() {
        // ...
    }
}
```
### 注册
在`resources/META-INF/kotlinmcui/entries`（**一个文件**）里声明，每行一条，环境必须手写：

```text
# <全限定类名> <环境>[,<环境>...]
a.b.c.Entry                  client,server,standalone
a.b.c.Entry$ClientPanel      client
a.b.c.Entry$ServerCommands   server
```

- 环境只能是`client`、`server`、`standalone`，**没有`*`、没有别名**；
- 空行与`#`开头的行跳过，其余每行必须恰好两列；
- 缺环境列、端名拼错、同一个类声明两次都会**直接抛错**（同文件或跨 jar 都算）。

嵌套的`object`用`$`连接（`a.b.c.Entry$Client`）。

这一格式与 java 的标准 service 文件不同：我们需要在**加载类之前**按环境过滤，而`ServiceLoader`只会给出已构造好的实例，且它的格式由 JDK 固定、放不下"环境"这一列。

### 注册错了会怎样
加载器对失败逐条隔离：类找不到、没实现`DslEntryService`、构造不出来（没有可用的无参构造器也没有`INSTANCE`，或者构造器、静态初始化块抛异常），都只让**这一个**入口出局，其余照常加载，游戏不会崩。

出局的条目不会静默消失：它会以一行**占位项**出现在`kotlinmcui-backend`配置页列表的**末尾**——比正常入口矮，没有图标和开关，也不受"显示代码入口"开关影响。行上写着类名、它声明成了哪种入口，以及一句原因（如`does not implement DslEntryService`、`class not found`、`constructor failed: RuntimeException`、`overlay id 'x' is already used by …`），完整异常仍然打在日志里。

这一条对 Overlay 尤其有用：Overlay 的启用状态是按`id`存在配置里的，入口坏了以前会静默什么都不画，现在能在列表里直接看到是哪个入口坏了、为什么。

## 环境
`forge`和`fabric`等加载器都会对环境进行区分，因为某些类只存在于Client或Server其中一侧，引用到不存在的类会在**类加载时**直接崩溃。`kotlinmcui`用同一套区分，但把它做成了注册文件里的一列：

| 端 | 含义 |
| --- | --- |
| `client` | 进程里有游戏客户端 |
| `server` | 进程里有游戏服务端 |
| `standalone` | 没有任何游戏端（独立窗口、独立服务、测试工具） |

三者互斥，一个进程只能是其中一个。声明了哪些端，这个类就只在那些环境被加载——**过滤发生在加载类之前**，所以声明`server`的类在客户端进程里连类都不会被加载。

推荐写法是**按端拆成独立的类**，而不是在一个类里写分支：

```kotlin
object Entry : DslEntryService, DslEntryGui {
    override val name = "Example"
    override val id = "example"
    override val icon = ImageHolder.empty
    override fun initialize() {}
    context(ctx: DslContext) override fun gui() { /* ... */ }
}

// 引用客户端特有的类；注册为 client
object Client : DslEntryService {
    override val name = "Example Client"
    override val id = "example.client"
    override val icon = ImageHolder.empty
    override fun initialize() {}
}

// 引用服务端特有的类；注册为 server
object Server : DslEntryService {
    override val name = "Example Server"
    override val id = "example.server"
    override val icon = ImageHolder.empty
    override fun initialize() {}
}
```

**边界**：环境声明是**加载许可**，不是代码隔离。同一个类里混写两端专有的代码，在它被加载的那一端仍然会崩——框架是运行期注册表，做不到编译期检查。要真正在编译期隔离，得靠你自己的源集划分（Fabric 的`splitEnvironmentSourceSets`、Forge/NeoForge 的独立 client 包配`DistExecutor`）。

注意区分的是**进程形态**，不是"此刻在跑哪一侧的逻辑"：单人游戏的集成服务器跑在客户端进程内，因此那一侧仍是`client`。

## 阶段
只有两个阶段：

- `initialize()`：**装载期**。元数据可读，游戏客户端不可用，所以只能读事实、注册内容；**不要**在这里开窗、起服务或建根。
- `launch()`：**平台已发布、能建屏之后**。开窗、起服务、建根都放这里。

`launch()`由`DslRuntime.launch()`显式触发，不是发布平台时的副作用：测试与独立工具也会发布平台，但那时并没有游戏可启动。游戏内由后端在客户端/服务端初始化完成后调用一次，独立运行时由`main`自己调用。

## 种类
种类与阶段是两条独立的轴：实现哪个种类接口决定"提供什么"，注册文件里的环境决定"在哪些环境加载"。

### Gui
实现`DslEntryGui.gui()`，函数内容是完整的 ui。

```kotlin
context(ctx: DslContext)
override fun gui() {
    Row {
        Column {
            // ... 
        }
        Button {
            // ... 
        }
    }
    DefaultBackground {}
}
```
通过此方式注册的 gui 会显示在`kotlinmcui-backend`的配置页面中，点击可以进入这个界面对应的`Screen`。

### Overlay
实现`DslEntryOverlay.overlay()`，内容同上。通过此方式注册的 overlay 会在游戏界面顶层全屏叠加渲染，并且可以处理鼠标键盘等事件；需要在`kotlinmcui-backend`的配置页面内部手动开启后才会显示，开启顺序即层叠顺序。

### 不提供界面
两个种类接口都不实现也是合法的：那是一个"只做事"的入口（例如在`launch()`里打开一个窗口）。它不会出现在配置页的正常列表里，只在打开"显示代码入口"时可见。
