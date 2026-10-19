---
title: Image
nav_order: 1
parent: Components
---

# 图像
## `ImageHolder`
用来在前端和后端之间传递一个图像的引用。
- `id`：图像的字符串id，应该是唯一的。
- `width`、`height`：图像的宽高。应该以`1.px`为单位。是为了在渲染时计算纹理uv。

## 资源图像
可以手动构造，例子：`ImageHolder("minecraft:textures/block/dirt.png",16.px,16.px)`

也可以调用`imageResource()`，效果是一样的

即使使用了高清材质包，`dirt.png`变为`64x64`，这里也应该写成`16.px`。这里的宽/高只是用来计算渲染时的uv。
## 本地图像
可以用`dslPlatform.utils.localImage(file)`或 DSL 层的`localImage(file)`取本地文件里的图像。这个函数**应当立即返回**：文件还没加载好时返回`ImageHolder.empty`，加载完成后再次调用就会给出对应图片的 id。

**不要缓存返回值**——缓存会把"首帧还没加载好"变成"永远为空"。

本地文件变了以后，在**事件处理器**里（例如`clickable { }`）调用`reloadLocalImage(file)`刷新缓存。它没有返回值，也**不能写在页面体里**：它要求`DslExecuteContext`，而渲染上下文不提供，写在那儿编译不过。

## 渲染策略
`ImageStrategy`中有裁切、重复、拉伸等多种策略。使用时注意一些策略参数中的`uv:Rect`参数应当为相对于`ImageHolder`的矩形。例如如果我想渲染dirt的左上1/4，应该传入uv为`Rect(0.px,0.px,8.px,8.px)`。

`nineSlice`的`uvInner`是中心一块的矩形，`uvOuter`是整体占据的矩形。