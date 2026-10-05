---
title: Run and test
nav_order: 2
parent: Start
---

# 运行并测试
目的是检查依赖是否被正常加载。

按照各自加载器的教程，运行你的模组。进入游戏后打开模组列表页面，在里面应该可以找到`KotlinMCUI`和`KotlinMCUI-backend`两个模组。
如果是`fabric`，需要安装`modmenu`进入模组页面。

`KotlinMCUI-backend`应该有一个配置页面的入口。进入这个配置页面，你可以看到一些测试项目。观察它的行为是否正常。

如果展示不了测试页面，请重新检查之前的步骤。

如果你在开发**后端本身**而不是使用它，验证方式不一样：见[构建与运行](../backend/build)，那里用的是同一套预览机制。

## 预览自己的页面

预览的页面用**引用**指定，不需要注册任何东西——页面就是一个顶层函数：

```kotlin
context(ctx: DslContext)
fun MyPage() {
    Column { Text { "hello".emit() } }
}
```

打开它**不需要改构建脚本**，只要给游戏 JVM 一个属性。启动器直接开游戏时，把这句加进 JVM 参数：

```
-Dkotlinmcui.preview=com.example.mypackage.MyPage
```

用 Gradle 跑时借助 `JAVA_TOOL_OPTIONS`——这个变量由 JVM 自己读取，而游戏 JVM 会继承它，所以 `runClient` 无需任何配置就能带上：

```bash
JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=com.example.mypackage.MyPage" ./gradlew runClient
```

```powershell
$env:JAVA_TOOL_OPTIONS="-Dkotlinmcui.preview=com.example.mypackage.MyPage"; .\gradlew runClient
```

默认**只打开页面**，不截屏也不退出（适合自己看），日志里会打出 `preview: opened '<引用>' as <类#方法>`。再追加 `-Dkotlinmcui.preview.capture=true` 就变成给机器用的模式：拍一张截图写进 `run/screenshots/`、退出；引用解析不出来、页面构建或渲染时抛异常、截图没写出来，退出码都是非零。

引用的三种写法：IDE 里直接复制的顶层函数引用（`com.example.mypackage.MyPage`）、显式的 `包.类Kt#方法`、类名（用它的 `content()` 画，适合入口）。页面可以有额外参数，但**都要有默认值**——预览只能按默认值调用它们，返回值类型不限（带必填参数的函数不会被当成页面）。

预览**只画你指定的页面本身，不替你加背景**。控件自己没有底色，页面不画背景时游戏画面会直接透在控件后面。要原版风格的界面背景，就在页面根容器上挂 `defaultBackground()`：

```kotlin
context(ctx: DslContext)
fun MyPage() = Column { ... }.defaultBackground()
```

它铺的是所在容器的矩形（所以屏幕边缘仍会露出游戏画面），绘制在子组件之前，和[基础](../basic/index)里的 `DefaultBackground()` 是同一件事。

用完记得清掉那个变量（它对当前 shell 里**每个** JVM 都生效，包括 Gradle 守护进程）：JVM 会往 stderr 打两行 `Picked up JAVA_TOOL_OPTIONS: …`，那是正常的、无副作用。