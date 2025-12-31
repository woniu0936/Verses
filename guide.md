这是一个完整的实施文档。你可以直接参照此文档建立一个 Android Library Module，我们将这个库命名为 **`ComposeLikeAdapter`**。

---

# 📘 ComposeLikeAdapter 实施文档

## 1. 项目概述 (Project Overview)

**设计目标**：
在 Android View System (RecyclerView) 中实现类似 Jetpack Compose 的声明式 UI 构建体验。

**核心原则**：
*   **Zero Boilerplate**: 消除 Adapter、ViewHolder、ViewType 常量定义。
*   **High Performance**: 基于 `ListAdapter` 和 `DiffUtil` 实现智能差分更新。
*   **Type Safety**: 强依赖 ViewBinding，杜绝 `findViewById` 和类型转换异常。
*   **Flexible Layout**: 统一 API 支持 Linear、Grid、Staggered 布局及其混排。

**技术栈**：
*   Kotlin
*   AndroidX RecyclerView
*   ViewBinding

---

## 2. 模块配置 (Gradle Setup)

在你的 Library Module 的 `build.gradle.kts` 中：

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yourname.composeadapter" // 修改为你的包名
    // ... SDK 版本配置
    
    buildFeatures {
        viewBinding = true // 必须开启
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.12.0")
}
```

---

## 3. 核心代码实现 (Core Implementation)

请按照以下包结构创建文件。

### 3.1 基础模型 (`model/ItemWrapper.kt`)

这是列表中的最小渲染单元，不仅包含数据，还包含布局策略。

```kotlin
package com.yourname.composeadapter.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

// 通用的 ViewHolder
class SmartViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

// ViewBinding 工厂函数类型定义
typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

/**
 * 扁平化的渲染单元
 * @param id 用于 DiffUtil 识别 Item 是否是同一个对象
 * @param viewType 用于 RecyclerView 复用池的 Key
 * @param data 原始数据，用于 DiffUtil 对比内容变化
 * @param spanSize Grid 布局占用的列数
 * @param fullSpan 是否强制占满一行 (用于 Grid 和 Staggered)
 * @param factory ViewHolder 创建工厂
 * @param bind 数据绑定逻辑
 */
data class ItemWrapper(
    val id: Any,
    val viewType: Int,
    val data: Any,
    val spanSize: Int,
    val fullSpan: Boolean,
    val factory: (ViewGroup) -> SmartViewHolder,
    val bind: (SmartViewHolder) -> Unit
)
```

### 3.2 核心适配器 (`core/ComposeAdapter.kt`)

全能型 Adapter，处理 ViewType 缓存、Grid 跨度计算和瀑布流兼容。

```kotlin
package com.yourname.composeadapter.core

import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.yourname.composeadapter.model.ItemWrapper
import com.yourname.composeadapter.model.SmartViewHolder
import java.util.concurrent.atomic.AtomicInteger

class ComposeAdapter : ListAdapter<ItemWrapper, SmartViewHolder>(WrapperDiffCallback) {

    // ViewType 缓存池 (Key -> Int ID)
    // Key 通常是 Inflate 函数引用，或者用户指定的 contentType
    private val viewTypeCache = mutableMapOf<Any, Int>()
    private val typeCounter = AtomicInteger(0)

    /**
     * 获取或生成 ViewType ID
     * 保证同一个 Inflate 函数在多次渲染中对应同一个 ID，从而复用 ViewHolder
     */
    fun getOrCreateViewType(key: Any): Int {
        return viewTypeCache.getOrPut(key) { typeCounter.getAndIncrement() }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartViewHolder {
        // 根据 ViewType 找到对应的 Factory (从当前列表中找一个样本)
        val wrapper = currentList.first { it.viewType == viewType }
        return wrapper.factory(parent)
    }

    override fun onBindViewHolder(holder: SmartViewHolder, position: Int) {
        val item = getItem(position)
        
        // 特殊处理：瀑布流的 FullSpan 属性
        val params = holder.itemView.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            if (params.isFullSpan != item.fullSpan) {
                params.isFullSpan = item.fullSpan
            }
        }
        
        item.bind(holder)
    }

    // 给 GridLayoutManager 使用的辅助方法
    fun getSpanSize(position: Int, totalSpan: Int): Int {
        if (position !in 0 until itemCount) return 1
        val item = getItem(position)
        return if (item.fullSpan) totalSpan else item.spanSize
    }

    // 智能 Diff 策略
    object WrapperDiffCallback : DiffUtil.ItemCallback<ItemWrapper>() {
        override fun areItemsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemWrapper, newItem: ItemWrapper): Boolean {
            // 只要数据内容没变，就不触发重新绑定 (性能关键)
            return oldItem.data == newItem.data
        }
    }
}
```

### 3.3 DSL 构建域 (`dsl/ComposeScope.kt`)

这是库的灵魂，提供两套 API（极简 & 高级）。

```kotlin
package com.yourname.composeadapter.dsl

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.yourname.composeadapter.core.ComposeAdapter
import com.yourname.composeadapter.model.Inflate
import com.yourname.composeadapter.model.ItemWrapper
import com.yourname.composeadapter.model.SmartViewHolder

class ComposeScope(private val adapter: ComposeAdapter) {

    internal val newWrappers = mutableListOf<ItemWrapper>()

    // 临时上下文变量 (仅供高级模式使用)
    private var currentData: Any? = null
    private var currentId: Any? = null

    // =======================================================
    //  API 1.0: 极简模式 (直接传 Inflate)
    // =======================================================

    /**
     * 渲染列表数据
     */
    fun <T : Any, VB : ViewBinding> items(
        items: List<T>,
        inflate: Inflate<VB>,
        key: ((T) -> Any)? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB, T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            internalRender(
                inflate = inflate,
                contentType = null,
                data = item,
                id = key?.invoke(item) ?: index,
                span = span,
                fullSpan = fullSpan,
                onBind = { vb -> onBind(vb, item) }
            )
        }
    }

    /**
     * 渲染单个 Item (如 Header/Footer)
     */
    fun <VB : ViewBinding> item(
        inflate: Inflate<VB>,
        data: Any? = Unit,
        key: Any? = null,
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB) -> Unit = {}
    ) {
        internalRender(
            inflate = inflate,
            contentType = null,
            data = data ?: Unit,
            id = key ?: "single_${inflate.hashCode()}",
            span = span,
            fullSpan = fullSpan,
            onBind = onBind
        )
    }

    // =======================================================
    //  API 2.0: 高级模式 (控制流 + Render)
    // =======================================================

    /**
     * 遍历数据，配合 render 使用
     */
    fun <T : Any> items(
        items: List<T>,
        key: ((T) -> Any)? = null,
        block: ComposeScope.(T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            currentData = item
            currentId = key?.invoke(item) ?: index
            block(item)
        }
    }

    /**
     * 在 items 闭包内部调用，用于分支渲染
     */
    fun <VB : ViewBinding> render(
        inflate: Inflate<VB>,
        contentType: Any? = null, // 如果 inflate 是动态 lambda，必须传此 Key
        span: Int = 1,
        fullSpan: Boolean = false,
        onBind: (VB) -> Unit
    ) {
        internalRender(
            inflate = inflate,
            contentType = contentType,
            data = currentData ?: Unit,
            id = currentId ?: System.identityHashCode(currentData),
            span = span,
            fullSpan = fullSpan,
            onBind = onBind
        )
    }

    // =======================================================
    //  内部实现
    // =======================================================

    private fun <VB : ViewBinding> internalRender(
        inflate: Inflate<VB>,
        contentType: Any?,
        data: Any,
        id: Any,
        span: Int,
        fullSpan: Boolean,
        onBind: (VB) -> Unit
    ) {
        // 核心去重逻辑：优先用 contentType，否则用 inflate 函数引用
        val cacheKey = contentType ?: inflate
        val viewType = adapter.getOrCreateViewType(cacheKey)

        newWrappers.add(ItemWrapper(
            id = id,
            viewType = viewType,
            data = data,
            spanSize = span,
            fullSpan = fullSpan,
            factory = { parent -> 
                SmartViewHolder(inflate(LayoutInflater.from(parent.context), parent, false)) 
            },
            bind = { holder -> 
                @Suppress("UNCHECKED_CAST")
                onBind(holder.binding as VB) 
            }
        ))
    }
}
```

### 3.4 扩展入口 (`ext/RecyclerViewExt.kt`)

对外暴露的三个便捷函数。

```kotlin
package com.yourname.composeadapter.ext

import androidx.recyclerview.widget.*
import com.yourname.composeadapter.core.ComposeAdapter
import com.yourname.composeadapter.dsl.ComposeScope

/**
 * 1. 线性布局 (LinearLayoutManager)
 */
fun RecyclerView.compose(
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: ComposeScope.() -> Unit
) {
    val adapter = getOrCreateAdapter { 
        LinearLayoutManager(context, orientation, reverseLayout) 
    }
    submit(adapter, block)
}

/**
 * 2. 网格布局 (GridLayoutManager)
 */
fun RecyclerView.composeGrid(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    block: ComposeScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        GridLayoutManager(context, spanCount, orientation, reverseLayout).apply {
            // 自动绑定 SpanLookup
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                // 注意：这里需要再次获取 Adapter，因为闭包内的 adapter 可能是旧引用
                override fun getSpanSize(position: Int): Int {
                    val currentAdapter = this@composeGrid.adapter as? ComposeAdapter
                    return currentAdapter?.getSpanSize(position, spanCount) ?: 1
                }
            }
        }
    }
    submit(adapter, block)
}

/**
 * 3. 瀑布流布局 (StaggeredGridLayoutManager)
 */
fun RecyclerView.composeStaggered(
    spanCount: Int,
    orientation: Int = RecyclerView.VERTICAL,
    gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE,
    block: ComposeScope.() -> Unit
) {
    val adapter = getOrCreateAdapter {
        StaggeredGridLayoutManager(spanCount, orientation).apply {
            this.gapStrategy = gapStrategy
        }
    }
    submit(adapter, block)
}

// --- 私有辅助方法 ---

private fun RecyclerView.getOrCreateAdapter(
    createLayoutManager: () -> RecyclerView.LayoutManager
): ComposeAdapter {
    val currentAdapter = this.adapter as? ComposeAdapter
    if (currentAdapter != null) return currentAdapter

    val newAdapter = ComposeAdapter()
    this.adapter = newAdapter
    this.layoutManager = createLayoutManager()
    return newAdapter
}

private fun submit(adapter: ComposeAdapter, block: ComposeScope.() -> Unit) {
    val scope = ComposeScope(adapter)
    scope.block()
    // 提交数据给 ListAdapter 计算 Diff
    adapter.submitList(scope.newWrappers)
}
```

---

## 4. API 使用指南 (Usage Guide)

### 场景一：简单的线性列表

```kotlin
// 假设 ViewBinding: ItemUserBinding
recyclerView.compose {
    // 1. 顶部 Header
    item(ItemHeaderBinding::inflate) { binding ->
        binding.tvTitle.text = "用户列表"
    }

    // 2. 数据列表
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id } // 强烈建议提供 Key
    ) { binding, user ->
        binding.tvName.text = user.name
    }
}
```

### 场景二：复杂网格 (包含 Header, Grid, Banner)

```kotlin
recyclerView.composeGrid(spanCount = 4) { // 最小公倍数

    // 1. Banner (占满一行)
    item(
        inflate = ItemBannerBinding::inflate, 
        fullSpan = true,
        data = bannerUrl // 传入 data 以便 DiffUtil 感知变化
    ) { binding ->
        Glide.with(binding.root).load(bannerUrl)...
    }

    // 2. 菜单 Grid (每行 4 个)
    items(
        items = menus,
        inflate = ItemMenuBinding::inflate,
        span = 1
    ) { binding, menu ->
        binding.tvName.text = menu.name
    }
    
    // 3. 混合类型列表 (使用 render)
    items(feedList, key = { it.id }) { feed ->
        
        if (feed is Ad) {
            // 广告占满
            render(ItemAdBinding::inflate, fullSpan = true) { ... }
        } else if (feed is Product) {
            // 商品占一半 (一行2个)
            render(ItemProductBinding::inflate, span = 2) { ... }
        }
    }
}
```

### 场景三：瀑布流

```kotlin
recyclerView.composeStaggered(spanCount = 2) {
    items(images, ItemImageBinding::inflate) { binding, img ->
        // 动态高度
        binding.img.layoutParams.height = img.height
        binding.img.load(img.url)
    }
}
```

---

## 5. 性能与注意事项

1.  **ViewBinding 引用**: 始终推荐使用 `ItemBinding::inflate` 函数引用。这能保证 ViewType 被正确缓存和复用。
2.  **DiffUtil Key**: 在调用 `items` 时，尽量传入 `key` 参数（如 ID）。如果不传，默认使用 List 索引，这在发生删除/插入操作时会导致多余的绑定，甚至动画异常。
3.  **Item Data**: 如果 `item()` (单项) 的内容依赖外部变量，请务必将该变量传给 `data` 参数，否则 DiffUtil 会认为内容未变而不刷新 UI。
4.  **Lambda 陷阱**: 如果你在 `render` 中使用动态 lambda 作为 `inflate` 参数，**必须** 提供 `contentType` 参数作为手动去重 Key，否则会导致 ViewType 爆炸。

---

你可以将以上代码复制到 Android Studio 的 Library Module 中，即可得到一个**生产级**的 Compose-like RecyclerView 库。