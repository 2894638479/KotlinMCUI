# 三角形列表并入 DslBackendRenderer

## 背景

绘制原语最初只有一个四边形，三角形列表是**可选能力**：单独一个 `DslBackendTriangles<RP>` 接口，加一个 `DslBackendRenderer<RP>.triangles()` 扩展去取，取不到 `null` 就不画。当时的理由是"接口必须最小"与"框架自身不调用它"，同时也考虑了平台成本——三角形列表在 1.20.1 上只能走公开的立即模式（`RenderType` 没有三角形类型、`create` 不公开），在 26.3 上要自己注册管线。

## 决定

三角形列表并入 `DslBackendRenderer`，作为**必实现**的第二个绘制成员：

```kotlin
context(renderParam: RP)
fun renderTriangles(vertices: FloatArray, colors: IntArray, image: ImageHolder? = null)
```

`DslBackendTriangles` 接口与 `triangles()` 扩展一并删除；便捷扩展 `fillPolygon` 改为挂在 `DslBackendRenderer` 上。

## 理由

- **可选能力会掩盖"声明了但实现是坏的"。** 7 条后端全都实现了这条能力，其中一条（neoforge 1.21.1）实际一个像素都不画——可选接口让这件事既不编译报错也不测试失败，最后靠人眼在能力页上才发现。强制实现把同类问题提前到编译期。
- **框架自己需要它。** 三角取色板是框架组件，不是用户组件；原先它在没有能力时静默隐藏模式，等于把一个框架特性变成后端的能力差异。
- **调用点变简单。** 组件侧不再需要 `?: return`，也没有"取不到就不画"这条契约要解释。
- 平台成本没有变：需要立即模式的平台本来就要写这段代码，唯一的区别是它现在是编译期要求而不是运行期可选。

## 影响

- 后端实现 `renderTriangles` 成为契约的一部分；`docs/backend/porting.md` 的移植清单包含它。
- 绘制成员从 1 个变成 2 个，零分配约束对两者都成立（三角形列表的数组由调用方复用，延迟绘制的后端必须自己拷一份）。
- 能力页（`test.TestBackendRenderPage`）里那一格从"没有能力显示灰块"变成单纯的三角形自检；灰块分支被删除。

## 被否的备选

- **保持可选，只补文档与验收。** 缺点是不解决"坏实现无人发现"这个已经发生过的问题；优点是第三方后端可以只实现四边形。此方案曾被上一轮建议过，这一轮被推翻。
- **在 `DslBackendRenderer` 里给 `renderTriangles` 一个默认实现。** 想做也做不到：默认实现只能建在 `renderQuad` 上，而四边形原语的形状来自 `rect`，表达不了三角形。

## 后续改名（2026-10-18）

本篇正文保留写作当时的类型名：`DslBackendRenderer` 现在叫 `DslRootRenderer`（同一类型，纯改名，无行为变化）；更早的 `DslBackendTriangles` 在本决定作出后即被删除，从未改名。**ADR 记录的是当时的决定，不随代码改名重写正文**；旧名只在这条注记里出现一次，用于指认。
