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
可以手动构造，例子：`ImageHolder(minecraft:textures/block/dirt.png,16.px,16.px)`

也可以调用`imageResource()`，效果是一样的

即使使用了高清材质包，`dirt.png`变为`64x64`，这里也应该写成`16.px`。这里的宽/高只是用来计算渲染时的uv。
## 本地图像
可以用`backend.loadLocalImage()`或`imageFile()`来加载本地文件中的图像。这个函数应当立即返回。如果这个文件还没加载好，应该返回`ImageHolder.empty`。可以放心把它传给其它函数。加载完成后，会返回对应图片的id。如果本地文件发生了变化，可以调用`dslBackend.forceLoadLocalImage()`来刷新缓存或者预加载。

## 渲染策略
`ImageStrategy`中有裁切、重复、拉伸等多种策略。使用时注意一些策略参数中的`uv:Rect`参数应当为相对于`ImageHolder`的矩形。例如如果我想渲染dirt的左上1/4，应该传入uv为`Rect(0.px,0.px,8.px,8.px)`。

`nineSlice`的`uvInner`是中心一块的矩形，`uvOuter`是整体占据的矩形。