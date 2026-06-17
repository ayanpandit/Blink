# Blink Technology Stack

To achieve the performance target of a `< 500ms` cold start and a `< 300ms` document opening speed, the technology stack has been selected with a strict focus on low runtime overhead, offline capability, and high drawing performance.

---

## Core Technologies

### 1. Kotlin & Jetpack Compose
* **Why Chosen**: Kotlin provides modern language features (inline classes, extension functions) that allow for writing clean, zero-cost abstractions. Jetpack Compose provides a declarative UI framework that eliminates the XML layout parsing overhead during startup. Compose's custom `Canvas` drawing and virtualized lists (`LazyColumn`, `LazyRow`) allow us to render complex pages smoothly at 60 FPS.
* **Alternatives Rejected**: Standard Android XML Views. XML parsing is CPU-intensive and occurs on the main thread during activity inflation, which conflicts with our startup targets.

### 2. Kotlin Coroutines & Flow
* **Why Chosen**: Coroutines allow lightweight thread switching to perform heavy decoding off the main thread. `Flow` (specifically `SharedFlow` and `StateFlow`) is utilized to stream document loading progress, text search results, and page bitmap rendering states reactively without memory leaks.
* **Alternatives Rejected**: RxJava. RxJava has a massive method count, high classloading overhead, and creates significant allocation pressure on the garbage collector during stream creation.

### 3. PDFium (Native C++)
* **Why Chosen**: PDFium is the high-performance native C++ PDF engine used by Google Chrome. By wrapping it via JNI, we can perform extremely fast rendering directly into bitmap memory buffers.
* **Alternatives Rejected**: Android's native `PdfRenderer`. While `PdfRenderer` requires no external library size, it suffers from several limitations: it does not support password-protected files natively on older Android versions, has multi-threading constraints, and lacks optimization hooks for custom rendering tiles. 

### 4. Apache POI
* **Why Chosen**: Apache POI is the most mature, feature-rich library for reading Microsoft Office formats (DOC, DOCX, XLS, XLSX, PPT, PPTX) in Java/Kotlin.
* **Alternatives Rejected**: 
  * **Online Document Renderers (Google/Microsoft WebView)**: Rejected because they require internet connectivity. Blink is strictly offline-first.
  * **docx4j / JODConverter**: docx4j requires JAXB which adds massive runtime overhead and size. JODConverter requires a running LibreOffice daemon, which is impossible on standard Android.
* **Optimization Strategy**: Apache POI is large. We will use an optimized Android port (e.g., custom compilation or a lightweight fork) and apply aggressive R8/ProGuard minimization rules to strip unused classes (such as writing features, charts, and complex formulas) to keep the final APK size under control.

### 5. Coil (Coroutine Image Loader)
* **Why Chosen**: Coil is built natively on Kotlin Coroutines, leading to less overhead and smaller binary footprint compared to older image loading libraries. We use Coil to display cached page thumbnails and parse inline images embedded within office documents.
* **Alternatives Rejected**: Glide / Picasso. Glide is feature-heavy but adds significant method count and uses annotation processing which increases build time and adds startup overhead.

---

## Explicitly Excluded Technologies

| Excluded Tech | Why Excluded | Better Alternative |
|---|---|---|
| **Firebase / Supabase** | High initialization overhead, requires network connectivity, adds background service processes that slow down cold start. | **None**. Blink is an offline-first app that does not require user accounts or cloud storage. |
| **Room Database** | Relational database initialization (schema verification, SQLite connection opening) blocks the main thread and introduces unwanted startup latency. | **Proto DataStore** / **SharedPreferences**. For basic local state persistence (e.g., search history, view settings), a lightweight key-value datastore is sufficient and runs asynchronously. |
| **Dagger / Hilt** | Dependency injection libraries compile code that triggers dynamic class loading and reflection during startup, increasing cold-start duration. | **Manual Dependency Injection**. We will manage the dependency graph manually via constructor injection and simple custom container classes initialized lazily. |
