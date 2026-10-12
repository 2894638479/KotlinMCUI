---
title: Window
nav_order: 5
parent: Backend
---

# 自带窗口的后端

这是**第二个官方后端**，也是一个独立的 mod（id `kotlinmcuiwindow`）：它自己开 Swing 窗口，用 Java2D 把界面画出来，事件也由它自己收。它**不引用任何 Minecraft 类**，因此一个 jar 同时覆盖所有游戏版本与三个加载器（jar 里同时带 fabric、forge、neoforge 三份元数据，各指向自己的入口类）。

它必须与[无界面后端](headless)一起装：无界面那份负责 `install`（元数据与入口发现），而它负责发布真正会渲染的后端。无界面后端发现它已安装时会保持沉默——这就是两者的分工。

## 它做什么、不做什么

做：自己的窗口（一个页面一个窗口）、把 AWT 的鼠标与键盘事件翻译成框架事件、用 Java2D 画框架的全部绘制原语、读写系统剪贴板、按 `capture` 截自己的窗口。

不做：不在游戏窗口里画任何东西（这是刻意的——正是这一点让它与游戏版本无关）；不读 Minecraft 的贴图与语言，`renderButton`/`renderSlot`/`renderContainer`/`renderTooltip`/`renderItem` 是它自己的平面近似，`translate` 返回 `null`（所以翻译键会原样显示）。

## 独立运行

不装游戏也能跑，这是开发和验证它的常规方式：

```
./gradlew run -Dkotlinmcuiwindow.capture=demo.png -Dkotlinmcuiwindow.capture.delay=2500
```

窗口会打开、渲染一个演示页，然后按 `capture` 指定的名字把图写进 `screenshots/` 并退出（不传 `capture` 就只是开窗，自己关）。

## 两条平台细节

- **`java.awt.headless`**：游戏进程启动时它是 `true`，而 AWT 会在第一次被使用时把这个值**缓存**下来，之后在同一个进程里 `JFrame` 只会抛 `HeadlessException`——异常抛在事件线程上、被 Swing 吞掉，表现为"窗口不出现且什么都不报"。因此这个 mod 在最早的入口（Fabric 的 `main`、Forge/NeoForge 的构造函数）里先把它清成 `false` 再碰任何 AWT 类。
- **预览用不了**：`-Dkotlinmcui.preview` 在打开页面前会等"平台已经渲染过一帧"，而这个帧恰恰要由窗口产生，窗口又要有页面才会建——循环等待。想看某个页面，直接自己显示它（或按上面的方式独立运行）。
