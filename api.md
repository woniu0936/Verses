这份文档是为库 **`com.woniu0936.verse`** 量身定制的 API 设计规范与使用手册。你可以将此文档直接作为 GitHub `README.md` 或官方 Wiki 使用。

---

# 🌌 Verse: Android RecyclerView 声明式构建库

**Verse** 是一个极简、高性能的 Android RecyclerView 扩展库。它引入了类似 Jetpack Compose 的 DSL 语法，让开发者能够以声明式的方式构建复杂的列表界面，彻底告别 Adapter、ViewHolder 和 ViewType 的繁琐样板代码。

*   **Package**: `com.woniu0936.verse`
*   **Min SDK**: 21
*   **Language**: 100% Kotlin

---

## 1. 核心入口 (Entry Points)

Verse 通过 Kotlin 扩展函数为 `RecyclerView` 提供了三个核心入口，分别对应 Android 的三大原生 LayoutManager。

### 1.1 线性布局 `compose`

适用于标准的垂直或水平列表（对应 `LinearLayoutManager`）。

```kotlin
fun RecyclerView.compose(
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
)
```

| 参数 | 类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `orientation` | `Int` | `VERTICAL` | 布局方向，`RecyclerView.VERTICAL` 或 `HORIZONTAL` |
| `reverseLayout` | `Boolean` | `false` | 是否反转布局（从底部开始） |
| `block` | `VerseScope.() -> Unit` | - | **核心 DSL 构建块**，在此处定义列表内容 |

### 1.2 网格布局 `composeGrid`

适用于网格列表（对应 `GridLayoutManager`）。Verse 会自动处理 SpanSizeLookup。

```kotlin
fun RecyclerView.composeGrid(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: VerseScope.() -> Unit
)
```

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `spanCount` | `Int` | **必填**。网格的列数（全局最小公倍数） |
| `block` | `VerseScope` | 在 DSL 中可通过 `span` 或 `fullSpan` 控制每个 Item 的大小 |

### 1.3 瀑布流布局 `composeStaggered`

适用于高度不固定的瀑布流列表（对应 `StaggeredGridLayoutManager`）。

```kotlin
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: VerseScope.() -> Unit
)
```

---

## 2. DSL 作用域 (VerseScope)

`VerseScope` 是构建列表内容的核心作用域。它提供了两套 API 风格：**极简模式**（针对简单的一对一映射）和 **控制流模式**（针对复杂的逻辑判断）。

### 2.1 极简模式 (Simple Mode)

适用于不需要复杂 `if/else` 判断的场景。

#### 🔹 `item` (单项)
渲染单个 Item（如 Header、Footer、Banner）。

```kotlin
fun <VB : ViewBinding> item(
    inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    data: Any? = Unit,
    key: Any? = null,
    span: Int = 1,
    fullSpan: Boolean = false,
    onBind: (VB) -> Unit = {}
)
```

*   **inflate**: `ViewBinding::inflate` 函数引用。
*   **data**: **关键**。Item 依赖的数据内容。如果内容变化（如 Title 变了），必须传入新值，否则 DiffUtil 认为未变化不刷新。
*   **key**: DiffUtil 的唯一标识。默认为 inflate 函数的 hash。建议显式指定（如 `"header"`）。
*   **fullSpan**: 在 Grid/Staggered 布局中是否强制占满一行。

#### 🔹 `items` (列表)
渲染一个数据集合，集合中的所有元素使用相同的布局。

```kotlin
fun <T : Any, VB : ViewBinding> items(
    items: List<T>,
    inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    key: ((T) -> Any)? = null,
    span: Int = 1,
    fullSpan: Boolean = false,
    onBind: (VB, T) -> Unit
)
```

*   **items**: 数据源列表。
*   **key**: Lambda，从数据对象中提取唯一 ID（如 `{ it.id }`）。**强烈建议提供**，以优化动画和性能。

---

### 2.2 控制流模式 (Control Flow Mode)

适用于混合类型列表（List\<Any>）或需要根据数据状态决定 UI 的场景。

#### 🔹 `items` (带 Block)
遍历列表，并在 Block 中使用 Kotlin 原生控制流（if/when）。

```kotlin
fun <T : Any> items(
    items: List<T>,
    key: ((T) -> Any)? = null,
    block: VerseScope.(T) -> Unit
)
```

#### 🔹 `render` (渲染指令)
在上述 `items` 的 block 内部调用，用于输出 UI。

```kotlin
fun <VB : ViewBinding> render(
    inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    contentType: Any? = null,
    span: Int = 1,
    fullSpan: Boolean = false,
    onBind: (VB) -> Unit
)
```

*   **contentType**: **去重键**。
    *   如果 `inflate` 传入的是函数引用（如 `ItemUserBinding::inflate`），此参数可为 `null`。
    *   如果 `inflate` 传入的是动态 Lambda，**必须**传入一个唯一的常量（String 或 Enum）作为 ViewType 的标识，否则会导致 ViewType 爆炸。

---

## 3. 类型定义 (Type Definitions)

为了代码简洁，库内部使用了以下别名，但在文档中展示完整签名以便理解。

```kotlin
// ViewBinding 工厂函数
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB
```

---

## 4. 使用示例 (Usage Examples)

### 4.1 基础线性列表

```kotlin
recyclerView.compose {
    // Header
    item(ItemHeaderBinding::inflate) { binding ->
        binding.title.text = "My List"
    }

    // List
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id }
    ) { binding, user ->
        binding.name.text = user.name
    }
}
```

### 4.2 复杂网格混排 (Grid + Mixed Types)

```kotlin
recyclerView.composeGrid(spanCount = 4) {
    
    // 1. Banner (占满一行)
    item(
        inflate = ItemBannerBinding::inflate, 
        fullSpan = true,
        data = bannerData // 传入数据以触发刷新
    ) { binding ->
        binding.img.load(bannerData.url)
    }

    // 2. 混合列表 (User占1格, Ad占满)
    items(feedList, key = { it.id }) { feed ->
        
        if (feed is User) {
            render(ItemUserBinding::inflate, span = 1) { binding ->
                binding.name.text = feed.name
            }
        } else if (feed is Ad) {
            render(ItemAdBinding::inflate, fullSpan = true) { binding ->
                binding.img.load(feed.url)
            }
        }
    }
}
```

---

## 5. 最佳实践与注意事项 (Best Practices)

### 5.1 DiffUtil 与性能
Verse 内部使用 `ListAdapter` (AsyncListDiffer)。
*   **Key**: 在调用 `items` 时，务必提供 `key` 参数。如果不提供，Verse 会使用列表索引（Index）作为 Key。这在发生删除、插入操作时会导致多余的 `onBind` 调用，且无法正确展示 Item 动画。
*   **Data**: 对于单个 `item()`，如果其 UI 依赖外部变量（如 `ViewModel.state.title`），请将该变量传给 `data` 参数：`item(..., data = state.title)`。否则 DiffUtil 比较新旧 Item 时会认为 `Unit == Unit`，从而跳过刷新。

### 5.2 ViewType 安全
*   **推荐**: 始终使用 **函数引用** (`ItemBinding::inflate`) 传递给 `inflate` 参数。Verse 内部会自动利用函数引用的稳定性来管理 ViewType 缓存。
*   **避免**: 避免在循环中传递 `inflate = { ... }` 这样的匿名 Lambda，除非你同时指定了 `contentType`。

### 5.3 滚动位置保持
Verse 复用了同一个 Adapter 实例。当数据发生变化重新调用 `compose` 时，只要 RecyclerView 已经绑定了 `VerseAdapter`，它只会计算 Diff 并更新数据，**不会**重置 Adapter。这意味着列表的滚动位置会自动保持，无需额外处理。

---

## 6. 混淆配置 (Proguard / R8)

Verse 强依赖 ViewBinding 的 `inflate` 函数引用。通常情况下，AGP 会自动处理 ViewBinding 的混淆规则。如果遇到 ViewBinding 类被移除的问题，请添加：

```pro
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(...);
}
```