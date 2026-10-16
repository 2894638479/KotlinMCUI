---
title: Window
nav_order: 5
parent: Backend
---

# 自带窗口的后端

这是**第二个官方后端**，也是一个独立的 mod（id `kotlinmcuiwindow`）：它自己开 Swing 窗口，用 Java2D 把界面画出来，事件也由它自己收。它**不引用任何 Minecraft 类**，因此一个 jar 同时覆盖所有游戏版本与三个加载器（jar 里同时带 fabric、forge、neoforge 三份元数据，各指向自己的入口类）。

它必须与[无界面后端](headless)一起装：无界面那份负责 `install`（元数据与入口发现），而它负责发布真正会渲染的后端。无界面后端发现它已安装时会保持沉默——这就是两者的分工。

## 一个窗口，页面叠在里面

窗口在客户端启动时**自己打开**，根页面就是框架的入口列表：客户端与叠加层入口都在里面，点一条进去。

- 进入子页面 = **入栈**：新页面盖住上一页，上一页留在栈里不动。
- 关闭或返回 = **出栈**：回到上一页。这与游戏后端的"关闭屏幕回到上一层"是同一个语义。
- 栈底那一页被关闭（或按 Esc）时窗口**收起**，不销毁：这里没有上一层可回，而这个窗口是框架在这台机器上的全部界面，没有按键、菜单或第二个窗口能把它叫回来。收起后点任务栏条目原样恢复，栈还在。
- 真正关掉窗口只有持有进程的一方做得到（`quit` / `closeAll`），因此后台运行不会留下一个打不开也关不掉的窗口。

鼠标与键盘事件由 AWT 收下后交给框架。界面按窗口大小缩放，拖拽窗口即重新排版。

## 它做什么、不做什么

做：自己的窗口与窗口内的页面栈、把 AWT 的鼠标与键盘事件翻译成框架事件、用 Java2D 画框架的绘制原语（含透视与三角形）、读写系统剪贴板、按 `capture` 截自己的窗口、解析 jar 里的语言文件与贴图资源、和游戏一起跑时按路径从客户端 jar 与 `mods/` 里读物品图标。

不做：不在游戏窗口里画任何东西（这是刻意的——正是这一点让它与游戏版本无关）；不引用任何 Minecraft 类，`renderButton`/`renderSlot`/`renderContainer`/`renderTooltip` 是它自己的平面近似。物品图标是其中唯一的例外：它不读 Minecraft 的代码，只读 jar 里的 PNG —— 版本客户端 jar 与 `mods/` 下的每个 jar 都按 `assets/<命名空间>/textures/item/<名字>.png` 找一遍，找不到（例如没有游戏的独立运行）就退回原来的平面替身。

## 独立运行

不装游戏也能跑。开发时用构建脚本：

```
./gradlew run -Pcapture=true -PcaptureName=demo.png
```

窗口会打开、渲染根页面，然后按 `capture` 指定的名字把图写进 `screenshots/` 并退出（不传 `capture` 就只是开窗，自己关）。

要直接看某一页并把图留下来，两个开关一起给：

```
./gradlew run -Ppreview=<引用> -Pcapture=true -PcaptureName=<文件>
```

预览会把那一页压在根页面之上，`capture` 让这个进程截完图自己退出；只给 `preview` 会一直开着窗口，只给 `capture` 截到的是根页面。

## 两个 jar，别拿错

`./gradlew build` 会出两份产物，来自同一份源码、功能一致，但用途相反：

| 产物 | 给谁 | 里面有什么 |
|---|---|---|
| `KotlinMCUI-window-<版本>.jar` | **加载器**（放进 `mods/`） | 三份加载器元数据各指向自己的入口；核心库是旁边的另一个 mod |
| `KotlinMCUI-window-<版本>-standalone.jar` | **不带游戏直接跑** | 自己的代码 + 核心库 + kotlin-reflect + 协程等；**没有**加载器元数据 |

```
java -jar KotlinMCUI-window-<版本>-standalone.jar
```

standalone 那份**不要**放进 `mods/`：它内含一份核心库，游戏里会和真正的核心库撞车（这也是它刻意不带加载器元数据的原因）。它认的是普通系统属性，与上面的 `-P` 开关一一对应：

```
java -Dkotlinmcui.preview=<引用> -Dkotlinmcui.preview.capture=true \
     -Dkotlinmcuiwindow.capture=<文件> -jar KotlinMCUI-window-<版本>-standalone.jar
```

## 三个平台细节

- **`java.awt.headless`**：游戏进程启动时它是 `true`，而 AWT 会在第一次被使用时把这个值**缓存**下来，之后在同一个进程里 `JFrame` 只会抛 `HeadlessException`——异常抛在事件线程上、被 Swing 吞掉，表现为"窗口不出现且什么都不报"。因此这个 mod 在最早的入口（Fabric 的 `main`、Forge/NeoForge 的构造函数）里先把它清成 `false` 再碰任何 AWT 类。
- **窗口尺寸来自第一页**：窗口按第一页声明的尺寸创建，之后可以自由拖拽；界面按新尺寸重新计算缩放，所以拖大拖小都不会留白或溢出。
- **窗口收起期间照样能截图**：`capture` 渲染的是窗口自己的组件树、而不是抓取桌面区域，所以窗口被最小化或收起时组件仍然存在，截图照常写出（实测三条路径——不动、外部最小化、走它自己的收起——产出的图逐字节相同）。这条限制只对"真实桌面抓屏"成立，而这里本来就没走那条路。
