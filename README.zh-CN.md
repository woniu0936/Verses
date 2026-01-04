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

- **🚀 性能巅峰**：基于 `ListAdapter` 配合专用后台线程池，处理万级数据依然丝滑。
- **🛡️ 工业级安全**：确定性 ViewType 生成（线性探测）+ 双层内存泄漏防护。
- **✨ 类 Compose 语法**：只写 UI，不写样板。彻底告别 `Adapter` 和 `ViewHolder`。
- **🧩 极高灵活性**：原生支持 `ViewBinding`、纯代码 `自定义 View` 以及复杂的多类型混合逻辑。
- **📦 隐式优化**：自动注入全局资源复用池，内置优化的刷新动画。

## 📦 安装

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.0.0-beta03")
}
```

## 📖 全能 API 与能力展示

Verses 提供统一的 DSL 来覆盖所有列表场景。

### 1. "全家桶" 示例
```kotlin
recyclerView.composeVerticalGrid(
    spanCount = 2,
    spacing = 16.dp,             // 内部间距
    contentPadding = 20.dp       // 外部边距
) {
    // A. 单个 ViewBinding 项目 (占满全行)
    item(ItemHeaderBinding::inflate, fullSpan = true) {
        tvTitle.text = "全功能演示"
    }

    // B. 纯代码构建的自定义 View
    item(create = { context -> MyCustomHeader(context) }) {
        // 'this' 即是 MyCustomHeader 实例
        setTitle("区域 A")
    }

    // C. 标准数据列表 (ViewBinding)
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id },
        span = 1
    ) { user ->
        tvName.text = user.name
        root.setOnClickListener { toast("点击了 ${user.name}") }
    }

    // D. 带业务逻辑的多类型渲染
    items(feedList, key = { it.id }) { feed ->
        when (feed) {
            is Banner -> render(ItemBannerBinding::inflate, fullSpan = true) {
                ivBanner.load(feed.url)
            }
            is Post -> render(ItemPostBinding::inflate) {
                tvContent.text = feed.text
            }
            is Video -> render(create = { context -> VideoPlayerView(context) }) {
                play(feed.videoUrl)
            }
        }
    }

    // E. 嵌套横向列表 (自动关联全局复用池)
    item(ItemHorizontalListBinding::inflate, fullSpan = true) {
        rvNested.composeRow(spacing = 8.dp) {
            items(categories, ItemCategoryBinding::inflate) { cat ->
                tvCategory.text = cat.name
            }
        }
    }
}
```

### 2. API 命名映射 (与 Compose 对标)

我们采用了与 Jetpack Compose 1:1 对标的命名，大幅降低学习成本。

| 布局类型 | 方向 | **Verses API** | **Jetpack Compose 对等项** |
| :--- | :--- | :--- | :--- |
| **线性** | 竖向 | **`composeColumn`** | `LazyColumn` |
| **线性** | 横向 | **`composeRow`** | `LazyRow` |
| **网格** | 竖向 | **`composeVerticalGrid`** | `LazyVerticalGrid` |
| **网格** | 横向 | **`composeHorizontalGrid`** | `LazyHorizontalGrid` |

### 3. 全局生命周期与资源管理
Verses 会在 View 分离或 Activity 销毁时自动清理。如需手动重置全局缓存（如退出登录时）：
```kotlin
VerseAdapter.clearRegistry()
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