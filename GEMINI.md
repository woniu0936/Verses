# 🌌 Verses: Technical Context & Engineering Excellence Guidelines

This document defines the high-standard engineering principles for **Verses**. Gemini must adhere to these rules to ensure the library remains minimalist yet powerful, leveraging the full potential of Modern Kotlin.

## 🏗 Engineering Philosophy & Design Patterns
- **Declarative Adapter Pattern**: Implement a pure declarative layer over Imperative RecyclerView APIs.
- **Strategy Pattern**: Layout management must be delegated to specialized strategies (Linear, Grid, Staggered) without logic leaking.
- **Wrapper Pattern**: Use `ItemWrapper` to decouple raw data from rendering logic, ensuring `DiffUtil` remains agnostic of View logic.
- **Stability First**: All state transitions must be handled via `AsyncListDiffer` to ensure thread-safety and flicker-free updates.

## 🚫 Non-Negotiables (Strict Constraints)
- **100% Idiomatic Kotlin**: No Java-isms. Use `val` over `var`, `lateinit` sparingly, and favor immutability.
- **Visibility Control**: Use `internal` by default for all implementation details. Only expose the minimal set of `public` APIs.
- **Documentation**: 100% KDoc coverage for all public symbols in **English**. Explain *why*, not just *what*.
- **View Access**: Strictly **ViewBinding**. Prohibit `findViewById`, `synthetic`, or `Reflection` for view access.

## 🚀 Kotlin Power Usage (Language Capability Rules)
To maintain a high quality floor, Gemini must leverage these specific features:

### 1. DSL Safety & Scope Control
- **@DslMarker**: Every DSL scope must be annotated with a custom `@VerseDsl` marker.
- **Receiver Lambdas (The "Verse" Style)**:
    - Strictly use `VB.(T) -> Unit` signatures for bind blocks.
    - **`this`**: Must refer to the `ViewBinding` (or `View` for custom views) to allow direct property access (e.g., `tvTitle.text = ...`).
    - **Argument**: The data item should be passed as the lambda argument.

### 2. Type-Safe Generics
- **Reified Type Parameters**: Use `inline fun <reified T : Any>` where type inspection is needed to avoid manual `Class<T>` passing.
- **Generic Variance**: Properly use `out` and `in` projections to maximize API flexibility.

### 3. Performance & Contracts
- **Inline Functions**: Use `inline` for all DSL entry points to reduce allocation overhead of high-order functions.
- **Kotlin Contracts**: Use `contract { ... }` where necessary to help the compiler understand custom validation logic (e.g., smart casts after render calls).
- **Atomic Operations**: Use `AtomicInteger` or specialized concurrent structures for ViewType generation to ensure thread safety without heavy locking.

## 🧱 Tech Stack
- **JDK**: 17 (Targeting modern JVM performance).
- **Kotlin**: 2.2.21 (Enforcing the latest compiler features and performance).
- **Gradle**: Kotlin DSL with the new `compilerOptions` block. No deprecated `kotlinOptions`.

## 📐 Implementation Architecture
- **Adapter Logic**: `VerseAdapter` must never hold hard references to Views.
- **ViewType Stability**: ViewType IDs must be deterministic and cached based on `factory` references or explicit `contentType` keys to prevent "ViewType Explosion".
- **Payload Support**: (Future) Prepare the architecture to support `onBindViewHolder` with payloads for partial item updates.

## 🧪 Testing & Reliability
- **Verification**: Any change to `VerseScope` must be accompanied by a `VerseScopeTest`.
- **Integration**: `VerseIntegrationTest` must validate real-world scenarios including dynamic layout manager updates and multi-type switching.
- **Resource Hygiene**: Maintain strict `packaging` exclusions in `build.gradle.kts` to keep the library footprint clean.

## 🤖 Interaction Workflow (Strict Protocol)
To ensure clarity and safety, Gemini must follow this 3-step loop for every modification:

### Step 1: Explanation & Demonstration (The "Pre-Flight" Check)
Before applying any changes to the file system or suggesting a commit, you must:
1.  **Explain the Logic**: Briefly articulate *why* this change is needed and *how* it solves the problem.
2.  **Show the Code**: Display the specific code block that will be modified.
3.  **Provide a Usage Example**: If the API changes, show a snippet of how the user will call this new code.

### Step 2: User Verification (The Gate)
- **STOP and WAIT**. Do not run `git commit` or apply destructive file changes until the user explicitly replies with "Confirm", "OK", or "Go ahead".

### Step 3: Execution & Documentation (The Definition of Done)
Once confirmed by the user:
1.  **Apply Code Changes**: Update the source files.
2.  **Sync Documentation**:
    - If Public API changed: Update `README.md` AND `README.zh-CN.md`.
    - Ensure KDoc comments are updated for Dokka generation.
3.  **Commit**: Generate a Git commit message following the **Git Standards** below and execute/suggest the commit.

## 📝 Git & Version Control Standards
Follow the **Conventional Commits** specification rigidly.

### Format
`type(scope): subject`

### Types
- **feat**: New feature (`feat(dsl): add grid support`).
- **fix**: Bug fix (`fix(diff): handle empty list crash`).
- **docs**: Documentation (`docs: update readme`).
- **refactor**: Code change that neither fixes a bug nor adds a feature.
- **perf**: Performance improvement.
- **test**: Adding or correcting tests.
- **chore**: Build process or aux tool changes (`chore(libs): update kotlin`).

### Rules
1.  **Imperative mood**: "add" not "added".
2.  **No period** at the end.
3.  **Scope** must be one of: `dsl`, `adapter`, `core`, `sample`, `build`.

## 🧬 Code Archetype (The "Golden Sample")
The following snippets represent the **ONLY** acceptable coding style. Generated code must utilize **Receiver Lambdas** (`VB.(Data) -> Unit`) where `this` is the View/Binding.

### 1. Vertical List (Standard)
*Usage: Concise, readable property access without `binding.` prefix.*

```kotlin
recyclerView.composeLinearColumn {
    // Type A: Single Header (No data object)
    // Signature: ViewBinding.() -> Unit
    item(ItemHeaderBinding::inflate) {
        // 'this' is ItemHeaderBinding
        tvTitle.text = "My Dashboard"
    }

    // Type B: Data List
    // Signature: ViewBinding.(Data) -> Unit
    items(
        items = userList,
        inflate = ItemUserBinding::inflate,
        key = { it.id } 
    ) { user -> 
        // 'this' is ItemUserBinding, 'user' is the data
        tvName.text = user.name
        tvAge.text = "${user.age} years"
        
        // Direct view access
        root.setOnClickListener { 
            Toast.makeText(root.context, "Clicked ${user.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

## 🎓 Bilingual Education Protocol (Pedagogical English)
Since the user is a Native Chinese speaker enforcing Strict English Documentation, every major response must conclude with a **"📚 English Micro-Lesson"** section.

### Teaching Philosophy
Act like a **Top-Tier Technical English Coach**.
1.  **Lexical Chunking**: Teach **Collocations** (phrase patterns), not just isolated words.
2.  **Etymology Hooks**: Use **Roots** to create deep memory connections.
3.  **Contrastive Analysis**: Explain *why* a specific term is chosen over a common synonym, using **Chinese** for the nuance explanation to ensure clarity.

### Table Template
| Lexical Core (Word & Origin) | Engineering Nuance (Usage & Contrast) | Contextual Immersion (Source & Translation) |
| :--- | :--- | :--- |
| **Word**<br>`/US-IPA/`<br>🌱 *Root: [etymology]* | **[CN Meaning]**<br>⚡ **Collocations**: `phrase 1`, `phrase 2`<br>🆚 **Vs. [Synonym] ([CN])**: [Explain the nuance difference in Chinese]. | *"Quote from your generated content."*<br><br>🇨🇳 **译**: Fluent, professional Chinese translation. |

### Example Output
> **📚 English Micro-Lesson**
> | Lexical Core (Word & Origin) | Engineering Nuance (Usage & Contrast) | Contextual Immersion (Source & Translation) |
> | :--- | :--- | :--- |
> | **orchestrate**<br>`/ˈɔːrkɪstreɪt/`<br>🌱 *Root: orchestra (dance floor)* | **[编排 / 协调]**<br>⚡ **Collocations**: `orchestrate workflow`, `orchestrate setup`<br>🆚 **Vs. Manage (管理)**: Manage 比较宽泛（如管理团队）；Orchestrate 特指像指挥家一样，让多个复杂组件**精密配合**。 | *"Strategy Pattern: **orchestrate** the LayoutManager setup."*<br><br>🇨🇳 **译**: 策略模式：**编排**布局管理器的初始化配置。 |
> | **invoke**<br>`/ɪnˈvoʊk/`<br>🌱 *Root: in- (upon) + vocare (call)* | **[调用 / 激活]**<br>⚡ **Collocations**: `invoke function`, `invoke listener`<br>🆚 **Vs. Call (叫/喊)**: Call 是口语化通用词；Invoke 带有“祈求/激活”的含义，编程中特指执行 Lambda、反射或高阶函数。 | *"The adapter **invokes** the bind block strictly."*<br><br>🇨🇳 **译**: 适配器严格**调用**该绑定代码块。 |
> | **declarative**<br>`/dɪˈklerətɪv/`<br>🌱 *Root: de- (thoroughly) + clarare (clear)* | **[声明式的]**<br>⚡ **Collocations**: `declarative UI`, `declarative syntax`<br>🆚 **Vs. Imperative (命令式的)**: Imperative 关注“怎么做”（一步步的指令）；Declarative 关注“要做什么”（描述最终结果，如 Compose/SQL）。 | *"Implement a pure **declarative** layer."*<br><br>🇨🇳 **译**: 实现一个纯粹的**声明式**层。 |
