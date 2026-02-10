---
title: Entrypoint
nav_order: 4
parent: Start
---
# 入口点
`kotlinmcui`提供一个加载器无关的入口点，并且二进制兼容。意味着你可以不再依赖`fabric/forge/neoforge`等加载器提供的入口点。

## 用法
创建`resources/META-INF/services/io.github.u2894638479.kotlinmcui.backend.DslEntryService`文件，向其中写入注册类的全限定名。

要求：这个类实现了`DslEntryService`接口，并且有一个无参构造函数。不要声明成`object`。
