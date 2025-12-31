# 🌌 Verse

**Verse** is a minimalist, high-performance declarative UI builder library for Android RecyclerView. It introduces a Jetpack Compose-like DSL syntax, allowing developers to build complex list interfaces without the boilerplate of Adapters, ViewHolders, and ViewType constants.

- **Zero Boilerplate**: No more Adapter/ViewHolder classes.
- **High Performance**: Built on `ListAdapter` and `DiffUtil` for smart asynchronous updates.
- **Type Safety**: Strictly powered by ViewBinding, eliminating `findViewById` and casting issues.
- **Flexible Layout**: Unified API for Linear, Grid, and Staggered layouts.

---

## 🚀 Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":verses"))
    // Ensure ViewBinding is enabled in your module
}
```

## 📖 Quick Start

### 1. Basic Linear List
```kotlin
// Vertical list (similar to LazyColumn)
recyclerView.composeLinearColumn {
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

// Horizontal list (similar to LazyRow)
recyclerView.composeLinearRow {
    items(tags, ItemTagBinding::inflate) { tag ->
        tvTag.text = tag
    }
}
```

### 2. Complex Grid Layout
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

### 3. Mixed Types with Logic
```kotlin
recyclerView.composeLinearColumn {
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

1. **ViewBinding Reference**: Always use function references like `ItemBinding::inflate`. This ensures ViewTypes are correctly cached and reused.
2. **Provide Keys**: Always provide a `key` in `items()` to enable smooth animations and prevent unnecessary re-binds.
3. **Item Data**: If a single `item()`'s content depends on external state (like a ViewModel variable), pass that variable to the `data` parameter to let `DiffUtil` know when to refresh.
4. **Lambda Caution**: If you use a dynamic lambda for `inflate` in `render()`, you **must** provide a `contentType` as a manual de-duplication key.

## 📄 License
MIT License.
