---
title: Image
nav_order: 1
parent: Components
---

# 图像
## `ImageHolder`
用来在前端和后端之间传递一个图像的引用。

- `source`：图像**从哪来**，见下节的四种来源；
- `width`、`height`：图像的宽高，以`1.px`为单位，渲染时用来计算纹理 uv。

## 三种来源，互不混淆

```kotlin
sealed interface ImageSource {
    data class Resource(val location: String)  // "minecraft:textures/block/dirt.png"
    data class LocalFile(val file: File)
    data class Remote(val url: String)         // http:// 或 https://
}
```

它们**是三种类型而不是三种字符串前缀**，所以不可能互相冒充：一个资源定位不可能被当成本地文件。解析由后端做 `when(source)`，不需要猜前缀。

"没有图像"不是来源的一种，而是 `ImageHolder.empty`（`source == null`）。本地/远程图片还没读完时也是 `empty`——**框架不区分"还没好"和"就是没有"**，两者对渲染都是"现在画不出东西"；加载失败由后端记日志，或者由后端换成一张占位**资源**再交回来。

`Resource` 的 `namespace:path` 与 `Remote` 的 `http(s)://` **不在构造时校验**：这些字符串可能来自配置文件、输入框或服务器，**写错不该抛异常**。解析不出来的资源由后端显示它自己的占位——Minecraft 显示紫黑方块；读不到的本地文件/网络图片则返回 `ImageHolder.empty`，什么都不画。

**画不出来是降级，不是崩溃。**

## 资源图像
```kotlin
imageResource("minecraft:textures/block/dirt.png", 16.px, 16.px)
```
即使使用了高清材质包、`dirt.png` 实际是 `64×64`，这里也应该写 `16.px`：宽高只是用来计算渲染 uv。

## 本地图像
```kotlin
val holder = localImage(file)          // 页面体里可以调
```
这个函数**应当立即返回**：文件还没读完时返回 `ImageHolder.empty`，之后再次调用会给出真正的holder（带 `ImageSource.LocalFile` 和真实尺寸）。

**不要缓存返回值**——缓存会把"首帧还没读完"变成"永远为空"。

本地文件变了以后，在**事件处理器**里（例如`clickable { }`）调用`reloadLocalImage(file)`刷新缓存。它没有返回值，也**不能写在页面体里**：它要求`DslExecuteContext`，而渲染上下文不提供，写在那儿编译不过。

## 网络图像
```kotlin
val holder = remoteImage("https://example.com/icon.png")
```
规则与本地图像相同：立即返回，`empty` 表示还没下载完，再调用即可。后端若没有 http 能力应当抛 `UnsupportedOperationException`，而不是永远回 `empty`——"不支持"和"还没加载好"不能长得一样。

内容变了、或者上次下载失败想重试，就在**事件处理器**里调 `reloadRemoteImage(url)`：同样没有返回值，同样不能写在页面体里。

## 渲染策略
`ImageStrategy`中有裁切、重复、拉伸等多种策略。使用时注意一些策略参数中的`uv:Rect`参数应当为相对于`ImageHolder`的矩形。例如如果我想渲染dirt的左上1/4，应该传入uv为`Rect(0.px,0.px,8.px,8.px)`。

`nineSlice`的`uvInner`是中心一块的矩形，`uvOuter`是整体占据的矩形。
