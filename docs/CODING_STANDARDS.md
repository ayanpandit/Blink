# Blink Coding Standards

These coding standards are designed to maintain high code quality, prevent performance bottlenecks, and ensure smooth rendering flows.

---

## 1. Naming Conventions

### General Naming
* **Classes & Objects**: `PascalCase` (e.g., `DocumentViewerViewModel`, `PdfDocument`).
* **Interfaces**: `PascalCase` without "I" prefix (e.g., `DocumentParser`, not `IDocumentParser`).
* **Variables & Properties**: `camelCase` (e.g., `currentPageIndex`, `pageWidth`).
* **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_CACHE_SIZE_BYTES`, `DEFAULT_PAGE_SCALE`).
* **Packages**: Lowercase, singular, without underscores (e.g., `com.blink.feature.pdf`, `com.blink.core.designsystem`).

### Jetpack Compose Naming
* **Composable Functions**: `PascalCase` and noun-based (e.g., `DocumentViewer`, `PageCanvas`).
* **Composables that return values**: Standard camelCase (e.g., `rememberPageLayoutState()`).
* **Preview Functions**: Prefixed with `Preview` followed by the target name (e.g., `PreviewDocumentViewer()`).

---

## 2. File and Package Organization

Every feature module follows a strict domain-driven package organization:

```
com.blink.[feature]
├── data
│   ├── model          # Data transfer objects or parser-specific models
│   └── parser         # Concrete implementations of parsers
├── domain
│   ├── model          # Domain business models
│   └── repository     # Interface definitions
└── presentation
    ├── components     # Local reusable composables
    ├── state          # UI state wrapper classes
    └── viewmodel      # Presenter ViewModels
```

* **Single Class per File**: Each Kotlin file must contain exactly one public class or interface. Exception: lightweight data classes or extensions closely related to the main class.
* **Extension Files**: Group extensions logically by type, named as `[Type]Ext.kt` (e.g., `UriExt.kt`, `ContextExt.kt`).

---

## 3. Jetpack Compose Performance Conventions

Compose UI must be coded with performance first to maintain >= 60 FPS scrolling.

* **Recomposition Skipping**:
  * All domain classes passed to Composable parameters must be marked with `@Immutable` or `@Stable` to allow Compose to skip recomposition.
  * Prefer passing primitive properties instead of full data classes to small sub-Composables.
* **Avoid Allocations in Composable Scope**:
  * Never instantiate objects (e.g., `Paint`, `Path`, `Regex`, or Collections) inside the body of a Composable function without wrapping them in `remember`.
* **State Read Optimization**:
  * Defer state reads to the layout or draw phase where possible. Use lambda-based modifiers when modifying attributes like offset, size, or alpha dynamically:
    ```kotlin
    // CORRECT (State read deferred to draw phase - avoids recomposition)
    Modifier.graphicsLayer { alpha = uiState.scrollAlpha }
    
    // INCORRECT (Triggers recomposition on every scroll change)
    Modifier.alpha(uiState.scrollAlpha)
    ```
* **Lists**:
  * Always provide a stable `key` inside `LazyColumn` or `LazyRow` items to ensure Compose recycles layout structures correctly:
    ```kotlin
    LazyColumn {
        items(
            items = pages,
            key = { page -> page.id }
        ) { page ->
            PageCard(page)
        }
    }
    ```

---

## 4. Kotlin & Coroutines Guidelines

* **Structured Concurrency**:
  * Never use `GlobalScope` or `runBlocking` in production code.
  * UI-related asynchronous actions must bind to `viewModelScope` or Jetpack Compose `LaunchedEffect` scopes.
  * Document rendering must run in custom, isolated dispatchers injected via constructors (e.g., `CoroutineDispatcher` mapped to `Dispatchers.IO` or custom bounded executors).
* **Avoid Thread Blocking**:
  * Ensure any blocking API call (e.g., `InputStream.read()`, POI parsing, native JNI calls) is executed on `Dispatchers.IO` or a custom dispatcher.
* **Allocation and GC Minimization**:
  * Avoid utilizing high-frequency functional operators (like `.map`, `.filter`, or `.flatMap`) inside rendering loops or canvas drawing loops. Use native loops (`for` or `forEach`) or pre-allocated arrays where appropriate.

---

## 5. Memory Safety & Resource Management

* **Closeable Handling**:
  * Always use the `.use {}` extension block when accessing file descriptors, input streams, and native cursors to guarantee resource release even in case of exceptions:
    ```kotlin
    contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
        // Safe access here
    }
    ```
* **Native Pointer Cleanup**:
  * JNI memory pointers (e.g., PDFium document reference handles) must be managed using lifecycle wrappers. Implement `AutoCloseable` on native-wrapper objects, and ensure they are recycled in ViewModel `onCleared()` or memory-eviction events.
