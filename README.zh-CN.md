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

## 📦 安装 (Installation)

```kotlin
dependencies {
    implementation("io.github.woniu0936:verses:1.1.0")
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
    item("header_1", ItemHeaderBinding::inflate, fullSpan = true) {
        tvTitle.text = "全功能演示"
    }

    // B. 纯代码构建的自定义 View
    item("header_2", create = { context -> MyCustomHeader(context) }) {
        // 'this' 即是 MyCustomHeader 实例
        setTitle("区域 A")
    }

    // C. 标准数据列表 (集成最佳实践)
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id },
        span = 1,
        // ✅ 整行点击：使用参数 (零对象分配)
        onClick = { user -> toast("点击了 ${user.name}") },
        // ✅ 子控件点击：使用 onCreate (一次性初始化)
        onCreate = {
            btnFollow.setOnClickListener {
                val user = itemData<User>() // 延迟获取数据
                viewModel.follow(user)
            }
        }
    ) { user ->
        // onBind：只负责更新视图状态
        tvName.text = user.name
        btnFollow.text = if (user.isFollowed) "取关" else "关注"
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
            is Post -> render(
                inflate = ItemPostBinding::inflate,
                onClick = { toast("动态: ${feed.text}") }
            ) {
                tvContent.text = feed.text
            }
        }
    }

    // E. 嵌套横向列表 (自动关联 Context 级复用池)
    item("horizontal_list", ItemHorizontalListBinding::inflate, fullSpan = true) {
        rvNested.composeRow(spacing = 8.dp, horizontalPadding = 16.dp) {
            items(categories, key = { it.id }, inflate = ItemCategoryBinding::inflate) { cat ->
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

### 3. 全局配置与诊断系统 (工业级能力)

Verses 提供了一套完备的诊断系统，帮助你调试复杂的列表行为并追踪线上错误。

#### A. 初始化 (Kotlin DSL)
在 `Application` 类中初始化 Verses 以启用全局能力：
```kotlin
Verses.initialize(this) {
    debug(true)           // 开启内部生命周期与 Diff 日志
    logTag("MyApp")       // 自定义 Logcat 标签
    logToFile(true)       // 开启本地文件日志用于排障分享
    
    // 生产环境错误遥测
    onError { throwable, message ->
        // 对接 Sentry / Bugly / Crashlytics
        Bugly.postCatchedException(throwable)
    }
}
```

#### B. Java 兼容性 (Builder 模式)
```java
VersesConfig config = new VersesConfig.Builder()
    .debug(true)
    .logToFile(true)
    .onError((throwable, msg) -> { /* 处理错误 */ })
    .build();
Verses.initialize(context, config);
```

#### C. 低成本排障
当用户反馈 Bug 时，你可以使用内置工具引导其分享诊断日志：
```kotlin
// 获取原始 Intent 以进行最大程度的自定义
val shareIntent = Verses.getShareLogIntent(context)
startActivity(Intent.createChooser(shareIntent, "分享日志"))

// 或者参考示例项目中的工具类：
// ShareUtils.shareLogFile(context)
```

### 4. 高级性能调优

Verses 引入了模型驱动架构与异步预加载技术，即使在极其复杂的布局下也能实现丝滑的 60 FPS。

#### A. 模型驱动架构 (VerseModel)
对于需要解耦 DSL 的复杂业务逻辑，你可以直接实现 `VerseModel`。

```kotlin
class MyCustomModel(id: Any, data: MyData) : ViewBindingModel<ItemUserBinding, MyData>(id, data) {
    override fun inflate(inflater: LayoutInflater, parent: ViewGroup) = 
        ItemUserBinding.inflate(inflater, parent, false)

    override fun bind(binding: ItemUserBinding, item: MyData) {
        binding.tvName.text = item.name
    }
    
    override fun getSpanSize(totalSpan: Int, position: Int) = 1
}
```

#### B. 异步预加载器 (VersePreloader)
通过在闲时（如等待网络请求时）预先解析 XML，彻底消除 `CreateViewHolder` 带来的卡顿。

```kotlin
// 为全局池预先填充 5 个特定类型的实例
VersePreloader.preload(
    context = this,
    models = listOf(
        MyCustomModel("template", MyData()),
        // ... 其他模版
    ),
    countPerType = 5
)
```

#### C. 在 DSL 中启用异步预加载
若要在 DSL 中使用 `VersePreloader`，必须手动提供 `layoutRes` 参数。

```kotlin
recyclerView.composeColumn {
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        layoutRes = R.layout.item_user, // 异步预加载必需
        key = { it.id }
    ) { user ->
        tvName.text = user.name
    }
}
```

#### D. 自动复用池优化
Verses 默认强制开启 **全局共享复用池 (Global Shared Pool)**。这意味着嵌套的 RecyclerView（如纵向列表中的横向滑动栏）将自动共用缓存，极大地降低内存占用与 View 创建开销。

### 5. 全局生命周期与资源管理
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
