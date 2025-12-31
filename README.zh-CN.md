# 🌌 Verse

**Verse** 是一个极简、高性能的 Android RecyclerView 声明式 UI 构建库。它引入了类似 Jetpack Compose 的 DSL 语法，让开发者能够以声明式的方式构建复杂的列表界面，彻底告别 Adapter、ViewHolder 和 ViewType 的繁琐样板代码。

- **零样板代码**: 无需定义 Adapter、ViewHolder 或 ViewType 常量。
- **高性能**: 基于 `ListAdapter` 和 `DiffUtil` 实现智能异步差分更新。
- **类型安全**: 强依赖 ViewBinding，杜绝 `findViewById` 和类型转换异常。
- **灵活布局**: 统一 API 支持 Linear、Grid、Staggered 布局及其混排。

---

## 🚀 安装

在你的模块 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation(project(":verses"))
    // 请确保你的模块已开启 ViewBinding
}
```

## 📖 快速上手

### 1. 基础线性列表
```kotlin
recyclerView.compose {
    // 单个 Item (如 Header)
    item(ItemHeaderBinding::inflate) { binding ->
        binding.tvTitle.text = "我的列表"
    }

    // 列表数据
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id } // 强烈建议提供 Key 以优化动画和性能
    ) { binding, user ->
        binding.tvName.text = user.name
    }
}
```

### 2. 复杂网格布局
```kotlin
recyclerView.composeGrid(spanCount = 4) {
    // 占满整行 (4列)
    item(ItemBannerBinding::inflate, fullSpan = true) { binding ->
        // 绑定 Banner
    }

    // 网格单元格 (默认占 1 列)
    items(productList, ItemProductBinding::inflate) { binding, product ->
        // 绑定商品
    }
}
```

### 3. 多类型混合逻辑
```kotlin
recyclerView.compose {
    items(feedList, key = { it.id }) { feed ->
        when (feed) {
            is User -> render(ItemUserBinding::inflate) { binding ->
                binding.name.text = feed.name
            }
            is Ad -> render(ItemAdBinding::inflate, fullSpan = true) { binding ->
                binding.img.load(feed.imageUrl)
            }
        }
    }
}
```

---

## 💡 最佳实践

1. **ViewBinding 引用**: 始终推荐使用 `ItemBinding::inflate` 函数引用。这能保证 ViewType 被正确缓存和复用。
2. **提供 Key**: 在调用 `items()` 时，务必提供 `key` 参数，这对于流畅的 Item 动画和减少不必要的 `onBind` 至关重要。
3. **Item Data**: 如果单个 `item()` 的内容依赖外部变量（如 ViewModel 中的状态），请将该变量传给 `data` 参数，以便 `DiffUtil` 感知内容变化并刷新。
4. **Lambda 陷阱**: 如果你在 `render()` 中使用动态 lambda 作为 `inflate` 参数，**必须**提供 `contentType` 参数作为手动去重 Key。

## 📄 许可证
MIT License.
