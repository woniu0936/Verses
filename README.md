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

In the modern Android ecosystem, why still choose a RecyclerView-based library?

- **🚀 Performance Peak**: Built on `ListAdapter` and `AsyncListDiffer` with a dedicated background thread pool. It handles 10,000+ items with zero jank.
- **🛡️ Industrial-Grade Safety**: 
    - **Deterministic ViewTypes**: Uses an Epoxy-inspired linear probing algorithm to ensure unique IDs across shared ViewPools.
    - **Memory Leaks Prevention**: Automatic, dual-layer disposal (Lifecycle-aware + Attachment-aware) to clear nested adapters and listeners.
- **✨ Compose-like Syntax**: Write UI, not boilerplate. No more `Adapter`, `ViewHolder`, or manual `ViewType` constants.
- **🧩 Extreme Flexibility**: Deeply integrates with `ViewBinding`, while providing first-class support for programmatic custom Views.
- **📦 Transparent Optimization**: Automatically injects global resource pools and optimizes item animations out of the box.

## 📦 Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.0.0-alpha6")
}
```

## 📖 Quick Start

### 1. Basic Column (ViewBinding)
```kotlin
recyclerView.composeLinearColumn(spacing = 16.dp) {
    // Single Header
    item(ItemHeaderBinding::inflate) {
        tvTitle.text = "My Dashboard"
    }

    // List of data
    items(userList, ItemUserBinding::inflate, key = { it.id }) { user ->
        tvName.text = user.name
        root.setOnClickListener { /* Click Handling */ }
    }
}
```

### 2. Multi-Type Grid
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

### 3. Programmatic Custom Views (No XML)
```kotlin
recyclerView.compose {
    items(tags, create = { context -> MyTagView(context) }) { tag ->
        // 'this' is MyTagView
        setData(tag)
    }
}
```

## 🛠 Advanced Features

### Global Registry Teardown
When undergoing a major state change (e.g., Logout), manually release all static references:
```kotlin
VerseAdapter.clearRegistry()
```

### Grid Span Control
Control how many columns an item occupies in a grid:
```kotlin
items(data, inflate, span = 2) { ... }
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