# 🌌 Verses

**Verses** is a minimalist, industrial-grade declarative UI engine for Android RecyclerView. It brings the expressive power of Jetpack Compose DSL to the mature and stable world of RecyclerView, enabling you to build complex, high-performance lists with 80% less code.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.woniu0936/verses)](https://search.maven.org/artifact/io.github.woniu0936/verses)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="screenshot/sample01.png" width="300" alt="Verses Sample 1" /><br>
        <i>Linear & Grid Mixed</i>
      </td>
      <td align="center">
        <img src="screenshot/sample02.png" width="300" alt="Verses Sample 2" /><br>
        <i>Nested Horizontal Lists</i>
      </td>
    </tr>
  </table>
</div>

## 💎 Why Verses?

- **🚀 Performance Peak**: Built on `ListAdapter` and `AsyncListDiffer` with a dedicated background thread pool.
- **🛡️ Industrial-Grade Safety**: Deterministic ViewTypes (Linear Probing) and dual-layer memory leak prevention.
- **✨ Compose-like Syntax**: Write UI, not boilerplate. No more `Adapter` or `ViewHolder`.
- **🧩 Extreme Flexibility**: Supports `ViewBinding`, programmatic `Custom Views`, and mixed-type logic.
- **📦 Transparent Optimization**: Auto-injects global resource pools and optimizes item animations.

## 📦 Installation

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.0.0-beta02")
}
```

## 📖 Complete API & Capability Showcase

Verses provides a unified DSL to handle all your list requirements.

### 1. The "Kitchen Sink" Example (Comprehensive)
```kotlin
recyclerView.composeVerticalGrid(
    spanCount = 2,
    spacing = 16.dp,             // Internal item spacing
    contentPadding = 20.dp       // Outer list padding
) {
    // A. Single ViewBinding Item (Full Span)
    item(ItemHeaderBinding::inflate, fullSpan = true) {
        tvTitle.text = "Comprehensive Demo"
    }

    // B. Custom View Item (Programmatic)
    item(create = { context -> MyCustomHeader(context) }) {
        // 'this' is MyCustomHeader
        setTitle("Section A")
    }

    // C. Standard List (ViewBinding)
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id },
        span = 1
    ) { user ->
        tvName.text = user.name
        root.setOnClickListener { toast("Clicked ${user.name}") }
    }

    // D. Multi-Type rendering with logic
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

    // E. Horizontal Nested List (Automatic Pool Optimization)
    item(ItemHorizontalListBinding::inflate, fullSpan = true) {
        rvNested.composeRow(spacing = 8.dp) {
            items(categories, ItemCategoryBinding::inflate) { cat ->
                tvCategory.text = cat.name
            }
        }
    }
}
```

### 2. API Naming Mapping (The Naming Mapping)

We have aligned our API naming 1:1 with Jetpack Compose (removing the "Lazy" prefix) to minimize cognitive load.

| Layout Type | Orientation | **Verse API** | **Jetpack Compose Equivalent** | Android Native Implementation |
| :--- | :--- | :--- | :--- | :--- |
| **Linear** | Vertical | **`composeColumn`** | `LazyColumn` | LinearLayoutManager (Vertical) |
| **Linear** | Horizontal | **`composeRow`** | `LazyRow` | LinearLayoutManager (Horizontal) |
| **Grid** | Vertical | **`composeVerticalGrid`** | `LazyVerticalGrid` | GridLayoutManager (Vertical) |
| **Grid** | Horizontal | **`composeHorizontalGrid`** | `LazyHorizontalGrid` | GridLayoutManager (Horizontal) |
| **Staggered** | Vertical | **`composeVerticalStaggeredGrid`** | `LazyVerticalStaggeredGrid` | StaggeredGridLayoutManager (Vertical) |
| **Staggered** | Horizontal | **`composeHorizontalStaggeredGrid`** | `LazyHorizontalStaggeredGrid` | StaggeredGridLayoutManager (Horizontal) |

### 3. Global Lifecycle & Resource Management
Verses automatically cleans up when the View is detached or the Activity is destroyed. To manually wipe all caches (e.g., on Logout):
```kotlin
VerseAdapter.clearRegistry()
```

License
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