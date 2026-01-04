我们的库名为 **Verse** (寓意诗篇，且致敬 Universe/Compose)，且核心卖点是 **“像写 Compose 一样写 RecyclerView”**，那么**彻底对标 Jetpack Compose 的命名规则**，能够极大降低用户的认知转换成本。

如果用户熟悉 `LazyColumn`，那么他看到 `composeColumn` 就能零思考上手。

以下是**完全对标 Jetpack Compose** 的 API 命名设计方案。

---

# 🌌 Verse DSL API Specification

**Module**: `com.woniu0936.verse.dsl`
**Class**: `VerseScope`

## 1. 设计规范 (Design Guidelines)

为了处理 ViewBinding/CustomView、单项/列表、简单/高级模式的组合，我们采用了 **正交化重载 (Orthogonal Overloading)** 策略。

### 1.1 参数排序标准 (Parameter Ordering Standard)
所有重载函数严格遵循以下参数顺序，以符合 Kotlin 尾随 Lambda (Trailing Lambda) 的惯用写法：

1.  **Core Input (核心输入)**: `items` (数据源) 或 `inflate/create` (视图工厂)。
2.  **Identity (标识)**: `key` (DiffUtil 唯一键)。
3.  **Dependencies (依赖)**: `data` (仅单项需显式传递)。
4.  **Layout Metadata (布局元数据)**: `span` (跨度), `fullSpan` (占满)。
5.  **Execution (执行逻辑)**: `onBind` (绑定逻辑) 或 `block` (作用域逻辑)。

### 1.2 类型安全策略 (Type Safety Strategy)
所有涉及视图创建的函数均使用 `inline` + `reified` 泛型。
*   **目的**：提取 `VB::class.java` 或 `V::class.java` 作为稳定的 ViewType Key。
*   **效果**：彻底防止因 Lambda 实例化导致的 ViewType 爆炸，无需用户手动管理 `contentType`。
* 
### 1.3 命名映射表 (The Naming Mapping)

我们将 `RecyclerView` 的扩展函数命名，与 Compose 的 `Lazy` 组件进行一对一映射。我们去掉了 "Lazy" 前缀（因为 RecyclerView 本来就是 Lazy 的），保留了核心的方向语义。

| 布局类型 | 方向 | **Verse API** | **对标 Compose API** | Android 原生实现 |
| :--- | :--- | :--- | :--- | :--- |
| **线性** | 竖向 | **`composeColumn`** | `LazyColumn` | LinearLayoutManager (Vertical) |
| **线性** | 横向 | **`composeRow`** | `LazyRow` | LinearLayoutManager (Horizontal) |
| **网格** | 竖向 | **`composeVerticalGrid`** | `LazyVerticalGrid` | GridLayoutManager (Vertical) |
| **网格** | 横向 | **`composeHorizontalGrid`** | `LazyHorizontalGrid` | GridLayoutManager (Horizontal) |
| **瀑布流** | 竖向 | **`composeVerticalStaggeredGrid`** | `LazyVerticalStaggeredGrid` | StaggeredGridLayoutManager (Vertical) |
| **瀑布流** | 横向 | **`composeHorizontalStaggeredGrid`** | `LazyHorizontalStaggeredGrid` | StaggeredGridLayoutManager (Horizontal) |

---

## 2. API 矩阵 (API Matrix)

`VerseScope` 包含 **3 类** 核心动词，每类包含 **2 种** 视图实现变体。

| 动词 (Verb) | 目标场景 | 变体 A: ViewBinding | 变体 B: Custom View |
| :--- | :--- | :--- | :--- |
| **`items`** | **1:1 列表**。最常用的标准列表。 | `items(List, Inflate, ...)` | `items(List, Creator, ...)` |
| **`item`** | **1:1 单项**。Header, Footer, Banner。 | `item(Inflate, ...)` | `item(Creator, ...)` |
| **`items`** | **1:N 遍历**。开启控制流 (if/else)。 | `items(List) { ... }` | *(通用，无变体)* |
| **`render`** | **手动渲染**。配合上述遍历使用。 | `render(Inflate, ...)` | `render(Creator, ...)` |

---

## 3. 完整代码实现 (VerseScope.kt)

```kotlin
package com.woniu0936.verse.dsl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.woniu0936.verse.core.VerseAdapter
import com.woniu0936.verse.model.*

/**
 * The core DSL scope for building RecyclerView content declaratively.
 *
 * This scope provides methods to define items, lists, and control flow structures.
 * It utilizes [reified] generics to ensure ViewType safety automatically.
 */
class VerseScope(private val adapter: VerseAdapter) {

    internal val newWrappers = mutableListOf<ItemWrapper>()

    // Context variables for Advanced Mode (items + render)
    private var currentData: Any? = null
    private var currentId: Any? = null

    // ============================================================================================
    //  Group 1: Standard List (1:1 Mapping)
    // ============================================================================================

    /**
     * Renders a list of items using [ViewBinding].
     *
     * @param items The data source list.
     * @param inflate The ViewBinding inflater reference (e.g., ItemUserBinding::inflate).
     * @param key A function to extract a stable ID for DiffUtil. Defaults to list index (not recommended for mutable lists).
     * @param span The number of columns this item occupies in a Grid layout. Default is 1.
     * @param fullSpan Whether this item should span the full width in Staggered layouts. Default is false.
     * @param onBind The binding logic block.
     */
    inline fun <T : Any, reified VB : ViewBinding> items(
        items: List<T>,
        noinline inflate: Inflate<VB>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: (VB, T) -> Unit
    ) {
        val stableKey = VB::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> SmartViewHolder(inflate(LayoutInflater.from(p.context), p, false)) },
                bind = { h -> onBind(h.binding as VB, item) },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
            )
        }
    }

    /**
     * Renders a list of items using a Custom [View].
     *
     * @param items The data source list.
     * @param create A factory function to create the View (e.g., ::MyView or { TextView(it) }).
     * @param key A function to extract a stable ID for DiffUtil.
     * @param span The number of columns this item occupies in a Grid layout.
     * @param fullSpan Whether this item should span the full width.
     * @param onBind The binding logic block.
     */
    inline fun <T : Any, reified V : View> items(
        items: List<T>,
        noinline create: ViewCreator<V>,
        noinline key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: (V, T) -> Unit
    ) {
        val stableKey = V::class.java
        items.forEachIndexed { index, item ->
            internalRender(
                factory = { p -> createSafeViewHolder(p, create) },
                bind = { h -> onBind(h.view as V, item) },
                key = stableKey,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan
            )
        }
    }

    // ============================================================================================
    //  Group 2: Single Item (Header / Footer / Static)
    // ============================================================================================

    /**
     * Renders a single item using [ViewBinding].
     *
     * @param inflate The ViewBinding inflater reference.
     * @param data The data dependency. **Crucial**: If UI depends on external state, pass it here to trigger DiffUtil updates.
     * @param key A stable ID for DiffUtil. Defaults to a hash of the inflater.
     * @param span The span size. Default is 1.
     * @param fullSpan Whether to span full width. Default is true for single items.
     * @param onBind The binding logic block.
     */
    inline fun <reified VB : ViewBinding> item(
        noinline inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onBind: (VB) -> Unit = {}
    ) {
        val stableKey = VB::class.java
        internalRender(
            factory = { p -> SmartViewHolder(inflate(LayoutInflater.from(p.context), p, false)) },
            bind = { h -> onBind(h.binding as VB) },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_vb_${stableKey.name}",
            span = span,
            fullSpan = fullSpan
        )
    }

    /**
     * Renders a single item using a Custom [View].
     *
     * @param create A factory function to create the View.
     * @param data The data dependency.
     * @param key A stable ID for DiffUtil.
     * @param span The span size.
     * @param fullSpan Whether to span full width. Default is true.
     * @param onBind The binding logic block.
     */
    inline fun <reified V : View> item(
        noinline create: ViewCreator<V>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = true,
        noinline onBind: (V) -> Unit = {}
    ) {
        val stableKey = V::class.java
        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { h -> onBind(h.view as V) },
            key = stableKey,
            data = data ?: Unit,
            id = key ?: "single_view_${stableKey.name}",
            span = span,
            fullSpan = fullSpan
        )
    }

    // ============================================================================================
    //  Group 3: Advanced Control Flow (Iterator + Render)
    // ============================================================================================

    /**
     * Starts an iteration scope for advanced scenarios (e.g., mixed types, if/else logic).
     * Must be used in conjunction with [render].
     *
     * @param items The data source list.
     * @param key A function to extract a stable ID.
     * @param block The control flow block where you call [render].
     */
    fun <T : Any> items(
        items: List<T>,
        key: ((T) -> Any)? = null,
        block: VerseScope.(T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            currentData = item
            currentId = key?.invoke(item) ?: index
            block(item)
        }
    }

    /**
     * Renders a UI unit within an advanced [items] block using [ViewBinding].
     *
     * @param inflate The ViewBinding inflater.
     * @param contentType An optional explicit key for ViewType pooling. Use only if needed (e.g., same binding, different pools).
     * @param span The span size.
     * @param fullSpan Whether to span full width.
     * @param onBind The binding logic.
     */
    inline fun <reified VB : ViewBinding> render(
        noinline inflate: Inflate<VB>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: (VB) -> Unit
    ) {
        val stableKey = contentType ?: VB::class.java
        val data = currentData ?: Unit
        
        internalRender(
            factory = { p -> SmartViewHolder(inflate(LayoutInflater.from(p.context), p, false)) },
            bind = { h -> onBind(h.binding as VB) },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan
        )
    }

    /**
     * Renders a UI unit within an advanced [items] block using a Custom [View].
     *
     * @param create The View creator.
     * @param contentType An optional explicit key for ViewType pooling.
     * @param span The span size.
     * @param fullSpan Whether to span full width.
     * @param onBind The binding logic.
     */
    inline fun <reified V : View> render(
        noinline create: ViewCreator<V>,
        contentType: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        noinline onBind: (V) -> Unit
    ) {
        val stableKey = contentType ?: V::class.java
        val data = currentData ?: Unit

        internalRender(
            factory = { p -> createSafeViewHolder(p, create) },
            bind = { h -> onBind(h.view as V) },
            key = stableKey,
            data = data,
            id = currentId ?: System.identityHashCode(data),
            span = span,
            fullSpan = fullSpan
        )
    }

    // ============================================================================================
    //  Internal Implementation (Private)
    // ============================================================================================

    @PublishedApi
    internal fun <V : View> createSafeViewHolder(parent: ViewGroup, create: ViewCreator<V>): SmartViewHolder {
        val view = create(parent.context)
        if (view.layoutParams == null) {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else if (view.layoutParams !is RecyclerView.LayoutParams) {
            view.layoutParams = RecyclerView.LayoutParams(view.layoutParams)
        }
        return SmartViewHolder(view, null)
    }

    @PublishedApi
    internal fun internalRender(
        factory: (ViewGroup) -> SmartViewHolder,
        bind: (SmartViewHolder) -> Unit,
        key: Any,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean
    ) {
        val viewType = adapter.getOrCreateViewType(key)
        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            span = span,
            fullSpan = fullSpan,
            factory = factory,
            bind = bind
        ))
    }
}
```