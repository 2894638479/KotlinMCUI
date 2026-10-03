---
title: Backend
nav_order: 6
---

# 后端
KotlinMCUI 只描述界面与交互，不接触任何平台类型；真正把界面画出来、把输入送进来的是**后端**。官方后端是独立仓库 [KotlinMCUI-backend](https://github.com/2894638479/KotlinMCUI-backend)，本仓库只保留它的文档。

这一章面向**使用与维护官方后端的人**：怎么拿到它、怎么在本地把它和核心库一起构建、怎么验证改动、以及移植到新游戏版本时要动哪些东西。

- [概览](overview)：后端是什么、有哪些版本、依赖什么
- [构建与运行](build)：本地联合构建、启动游戏、自动验证
- [移植](porting)：升级 Minecraft 版本时的检查清单

如果你是**从零写一个自己的后端**，那属于另一件事，见[制作一个后端](../advanced/create-backend)；本章只讲官方实现这条路径。
