---
title: 发布
nav_order: 30
parent: Dev
---

# 发布

**推 tag 是唯一会发布的事情。** 分支上的 push 只构建与测试（`ci.yml`），不发布。

## 为什么不再由"版本号变了"触发

旧流程在每次 push 时检查远端有没有 `v<版本>` 这个 tag，没有就发布一次。它有两个问题：

- **触发与发布无关**：任何 push 都可能发布，只要那个 tag 还不存在；
- **失败不可重试**：tag 由发布流程自己创建，一旦创建成功、而某个平台没传上去，下次运行会认为"已发布"而永久跳过。

## 怎么做一次发布

1. 改 `gradle.properties` 里的 `mod_version`，并在 `CHANGELOG.md` **加一节**（发布正文就取这个文件）；
2. 提交并 push 到 `master`，等 `ci` 绿；
3. 打 tag 并推：

   ```bash
   git tag -a v1.0.0-alpha.32 -m "1.0.0-alpha.32"
   git push origin v1.0.0-alpha.32       # 普通 git push 不会推 tag
   ```

4. `release` workflow 被触发：先跑 `assertReleaseTag`（tag 必须等于 `v<mod_version>`，不一致直接失败），再跑 `publishMods` 发到 Modrinth / CurseForge / GitHub。

## 失败了怎么办

**重跑 workflow**（Actions 页面 → Re-run jobs）。不要删 tag 重推：同一个版本重复上传到平台通常不友好，而重跑不会制造第二个版本。

只有"tag 打错了（指向错误的提交）"才需要删 tag 重来，那时也要先确认平台上没有那一版。
