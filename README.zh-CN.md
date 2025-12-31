# 🌌 Verse

**Verse** 是一个极简、高性能的 Android RecyclerView 声明式 UI 构建库。它引入了类似 Jetpack Compose 的 DSL 语法，让开发者能够以声明式的方式构建复杂的列表界面，彻底告别 Adapter、ViewHolder 和 ViewType 的繁琐样板代码。

- **零样板代码**: 无需定义 Adapter、ViewHolder 或 ViewType 常量。
- **泛型特化安全 (Reified Safety)**: 通过 Class 对象锁定 ViewType，自动根除“ViewType 爆炸”风险。
- **高性能**: 基于 `ListAdapter` 和 `DiffUtil` 实现智能异步差分更新。
- **类型安全**: 强依赖 ViewBinding 和 Kotlin 泛型，杜绝 `findViewById` 和类型转换异常。

---

## 🚀 安装

在你的模块 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation(project(":verses"))
}
```

## 📖 快速上手

### 1. 基础线性列表 (ViewBinding)
```kotlin
// 竖向列表
recyclerView.compose {
    // 单个 Item (如 Header)
    item(ItemHeaderBinding::inflate) {
        // 'this' 是 ItemHeaderBinding
        tvTitle.text = "我的列表"
    }

    // 列表数据
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id } 
    ) { user ->
        // 'this' 是 ItemUserBinding
        tvName.text = user.name
    }
}
```

### 2. 纯代码构建 View (无需 XML)
```kotlin
recyclerView.compose {
    items(
        items = tags,
        create = { context -> TextView(context).apply { textSize = 16f } }
    ) { tag ->
        // 'this' 是 TextView
        text = tag
    }
}
```

### 3. 网格与瀑布流布局
```kotlin
recyclerView.composeGrid(spanCount = 4) {
    // 占满整行 (4列)
    item(ItemBannerBinding::inflate, fullSpan = true) {
        // 绑定 Banner
    }

    // 网格单元格 (默认占 1 列)
    items(productList, ItemProductBinding::inflate) { product ->
        // 绑定商品
    }
}
```

### 4. 多类型混合逻辑
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

## 💡 最佳实践

1. **特化键 (Reified Keys)**: Verse 使用 `VB::class.java` 作为缓存键。这意味着即使你使用动态 lambda，只要返回的 View 类型一致，复用机制就能正常工作。
2. **提供 Key**: 在调用 `items()` 时，务必提供 `key` 参数，这对于流畅的 Item 动画至关重要。
3. **Item Data**: 如果单个 `item()` 的内容依赖外部变量，请将该变量传给 `data` 参数，以便 `DiffUtil` 感知内容变化。

## 📄 许可证
MIT License.