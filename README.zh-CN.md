# 🌌 Verses

**Verses** 是一个为 Android RecyclerView 打造的极简、工业级声明式 UI 引擎。它将 Jetpack Compose DSL 的表达力带到了成熟稳定的 RecyclerView 领域，让你能以减少 80% 代码量的代价，构建出复杂且高性能的列表。

[![Maven Central](https://img.shields.io/maven-central/v/io.github.woniu0936/verses)](https://search.maven.org/artifact/io.github.woniu0936/verses)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="screenshot/sample01.png" width="300" alt="Verses 示例 1" /><br>
        <i>线性与网格混合布局</i>
      </td>
      <td align="center">
        <img src="screenshot/sample02.png" width="300" alt="Verses 示例 2" /><br>
        <i>嵌套横向列表</i>
      </td>
    </tr>
  </table>
</div>

## 💎 为什么选择 Verses？

在现代 Android 生态中，为什么依然需要一个基于 RecyclerView 的库？

- **🚀 性能巅峰**：基于 `ListAdapter` 和 `AsyncListDiffer` 构建，配合专用的后台线程池。即使处理 10,000+ 条数据也能保持 0 卡顿。
- **🛡️ 工业级安全**：
    - **确定性 ViewType**：参考 Epoxy 的线性探测算法，确保在共享 ViewPool 场景下 ID 绝对唯一且稳定。
    - **内存泄漏防范**：全自动、双层销毁机制（生命周期感知 + 附件状态感知），自动清理嵌套适配器和观察者。
- **✨ 类似 Compose 的语法**：只写 UI，不写样板。告别 `Adapter`、`ViewHolder` 和手动定义的 `ViewType` 常量。
- **🧩 极高灵活性**：深度集成 `ViewBinding`，同时对纯代码构建的自定义 View 提供一流支持。
- **📦 隐式优化**：开箱即用，自动注入全局资源池，并优化了 Item 刷新动画。

## 📦 安装

在模块的 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.0.0-alpha6")
}
```

## 📖 快速上手

### 1. 基础纵向列表 (ViewBinding)
```kotlin
recyclerView.composeLinearColumn(spacing = 16.dp) {
    // 单个 Header
    item(ItemHeaderBinding::inflate) {
        tvTitle.text = "我的仪表盘"
    }

    // 列表数据
    items(userList, ItemUserBinding::inflate, key = { it.id }) { user ->
        tvName.text = user.name
        root.setOnClickListener { /* 处理点击 */ }
    }
}
```

### 2. 多类型混合网格
```kotlin
recyclerView.composeGrid(spanCount = 2) {
    items(feedList) { feed ->
        when (feed) {
            is Banner -> render(ItemBannerBinding::inflate, fullSpan = true) {
                ivBanner.load(feed.url)
            }
            is Post -> render(ItemPostBinding::inflate) {
                tvContent.text = feed.text
            }
        }
    }
}
```

### 3. 纯代码自定义 View (无需 XML)
```kotlin
recyclerView.compose {
    items(tags, create = { context -> MyTagView(context) }) { tag ->
        // 'this' 即是 MyTagView 实例
        setData(tag)
    }
}
```

## 🛠 进阶功能

### 全局注册表销毁
在发生重大状态变更（如退出登录）时，手动释放所有静态引用：
```kotlin
VerseAdapter.clearRegistry()
```

### 网格跨列控制
控制某个 Item 在网格中占据的列数：
```kotlin
items(data, inflate, span = 2) { ... }
```

开源协议
-------

    Copyright 2025 Woniu0936

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
