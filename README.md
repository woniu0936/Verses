# 🌌 Verse

**Verse** is a minimalist, high-performance declarative UI builder library for Android RecyclerView. It introduces a Jetpack Compose-like DSL syntax, allowing developers to build complex list interfaces without the boilerplate of Adapters, ViewHolders, and ViewType constants.

- **Zero Boilerplate**: No more Adapter/ViewHolder classes.
- **Reified Safety**: Automatically prevents "ViewType Explosion" by using Class-based keys.
- **High Performance**: Built on `ListAdapter` and `DiffUtil` for smart asynchronous updates.
- **Type Safety**: Powered by ViewBinding and Generic types, eliminating `findViewById`.

<p align="center">
  <img src="./screenshot/sample.mp4" width="300" />
  <br>
  <i>Complex mixed layouts (Grid, Linear, Nested) built with Verses.</i>
</p>

---

## 🚀 Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":verses"))
}
```

## 📖 Quick Start

### 1. Basic Linear List (ViewBinding)
```kotlin
// Vertical list
recyclerView.compose {
    // Single Item (Header)
    item(ItemHeaderBinding::inflate) {
        // 'this' is ItemHeaderBinding
        tvTitle.text = "My List"
    }

    // List of Items
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id } 
    ) { user ->
        // 'this' is ItemUserBinding
        tvName.text = user.name
    }
}
```

### 2. Custom View Support (No XML)
```kotlin
recyclerView.compose {
    items(
        items = tags,
        create = { context -> TextView(context).apply { textSize = 16f } }
    ) { tag ->
        // 'this' is TextView
        text = tag
    }
}
```

### 3. Grid & Staggered Layouts
```kotlin
recyclerView.composeGrid(spanCount = 4) {
    // Item spans across all 4 columns
    item(ItemBannerBinding::inflate, fullSpan = true) {
        // bind banner
    }

    // Individual grid items (default span = 1)
    items(productList, ItemProductBinding::inflate) { product ->
        // bind product
    }
}
```

### 4. Mixed Types with Logic
```kotlin
recyclerView.compose {
    items(feedList, key = { it.id }) { feed ->
        when (feed) {
            is User -> render(ItemUserBinding::inflate) {
                name.text = feed.name
            }
            is Ad -> render(ItemAdBinding::inflate, fullSpan = true) {
                img.load(feed.imageUrl)
            }
        }
    }
}
```

---

## 💡 Best Practices

1. **Reified Keys**: Verse uses `VB::class.java` or `V::class.java` as ViewType keys. This means even if you use dynamic lambdas, recycling remains stable as long as the View class is the same.
2. **Provide Keys**: Always provide a `key` in `items()` to enable smooth animations and efficient `DiffUtil` calculations.
3. **Item Data**: If a single `item()` depends on external state, pass it to the `data` parameter to trigger updates.

License
-------

    Copyright 2025 Woniu0936

    Licensed under the MIT License (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://opensource.org/licenses/MIT

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
