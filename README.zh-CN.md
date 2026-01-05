# 🌌 Verses

中文 | [English](README.md)

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
- **🛡️ 工业级安全**：实例级工厂（Instance-local factories）与线程安全的 ViewType 生成，彻底杜绝 Context 泄漏。
- **✨ 类 Compose 语法**：只写 UI，不写样板。彻底告别手动编写 `Adapter` 或 `ViewHolder` 子类。
- **🧩 极高灵活性**：原生支持 `ViewBinding`、`自定义 View` 以及通过 `contentType` 区分的多样式逻辑。
- **📦 隐式优化**：Context 隔离的全局资源复用池，在多 Fragment/Activity 间自动优化内存性能。

## 📦 安装

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.0.0")
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
            // 使用 'contentType' 区分同一 Binding 类的不同样式，防止缓存冲突
            is Ad -> render(ItemPostBinding::inflate, contentType = "ad_style") {
                tvContent.text = "赞助商: ${feed.text}"
                root.setBackgroundColor(Color.YELLOW)
            }
            is Post -> render(ItemPostBinding::inflate) {
                tvContent.text = feed.text
            }
        }
    }

    // E. 嵌套横向列表 (自动关联 Context 级复用池)
    item(ItemHorizontalListBinding::inflate, fullSpan = true) {
        rvNested.composeRow(spacing = 8.dp, horizontalPadding = 16.dp) {
            items(categories, ItemCategoryBinding::inflate) { cat ->
                tvCategory.text = cat.name
            }
        }
    }
}
```

### 2. API Naming Mapping (与 Compose 对标)

我们采用了与 Jetpack Compose 1:1 对标的命名，大幅降低学习成本。

| 原生 RecyclerView | 方向 | **Verses API** | **Jetpack Compose 对等项** |
| :--- | :--- | :--- | :--- |
| `线性 (LinearLayoutManager)` | 竖向 | **`composeColumn`** | `LazyColumn` |
| `线性 (LinearLayoutManager)` | 横向 | **`composeRow`** | `LazyRow` |
| `网格 (GridLayoutManager)` | 竖向 | **`composeVerticalGrid`** | `LazyVerticalGrid` |
| `网格 (GridLayoutManager)` | 横向 | **`composeHorizontalGrid`** | `LazyHorizontalGrid` |
| `瀑布流 (StaggeredGridLayoutManager)` | 竖向 | **`composeVerticalStaggeredGrid`** | `LazyVerticalStaggeredGrid` |
| `瀑布流 (StaggeredGridLayoutManager)` | 横向 | **`composeHorizontalStaggeredGrid`** | `LazyHorizontalStaggeredGrid` |

### 3. 全局生命周期与资源管理
Verses 会在 View 分离或 Activity 销毁时自动清理。如需手动重置全局注册表（如退出登录时）：
```kotlin
VerseAdapter.clearRegistry()
```

### ⚠️ 性能与更新说明
`onBind` 和 `onClick` 逻辑的更新完全依赖于 `data` 的变化。如果 `data` 的 `equals` 返回 true，UI 将不会触发重新绑定。若需强制刷新，请使用 `data.copy()`。

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
