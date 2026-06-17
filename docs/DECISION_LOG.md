# Blink Architectural Decision Log

This log lists significant architectural decisions made during the design of Blink.

---

## 2026-06-17: Manual Dependency Injection over Dagger Hilt or Koin

* **Decision**: Avoid utilizing automated Dependency Injection (DI) frameworks (Hilt, Dagger, Koin). Use manual constructor-based dependency injection.
* **Reason**: Dagger Hilt introduces compiler boilerplate, reflection hooks, and initialization steps that increase App startup time. Koin uses service-locator maps that can cause slight reflection slowdowns on low-end devices. Manual DI gives us absolute control, compiling to direct constructor invocations with zero overhead.
* **Impact**: Developers must manually construct ViewModels and Repositories inside custom `ViewModelProvider.Factory` structures, passing dependency chains explicitly. Service containers are initialized lazily inside `BlinkApplication`.

---

## 2026-06-17: Format-Based Modularization

* **Decision**: Modularize the feature layer by file format types (e.g., `:feature:pdf`, `:feature:office`, `:feature:text`).
* **Reason**: Separating format engines prevents the JVM classloader from loading large libraries into memory when they aren't needed. For example, when opening a `.pdf` file, Apache POI classes in the `:feature:office` module do not need to be loaded, saving memory and decreasing launch latency.
* **Impact**: Strict separation of format configurations. Inter-feature communication must be routed through `:app` or unified core interfaces defined in `:core:document`.

---

## 2026-06-17: Omission of Room Database

* **Decision**: Do not include a relational Room/SQLite database for V1.
* **Reason**: Room database creation requires schema checks, SQLite file locks, and disk access during app initialization. This blocks the main thread and exceeds our `< 500ms` startup budget.
* **Impact**: Blink V1 operates purely in-memory on local file inputs. If persistent settings or file history are introduced in future releases, we will use Jetpack Proto DataStore, which runs asynchronously.

---

## 2026-06-17: Native PDFium Wrapper

* **Decision**: Use PDFium via direct JNI bindings rather than Android's native `PdfRenderer` class.
* **Reason**: Android's standard `PdfRenderer` requires a thread-locked FileDescriptor, has poor rendering performance on older API levels, and cannot handle password-protected documents natively. PDFium is thread-safe, handles custom rendering targets, and is optimized for low memory usage.
* **Impact**: Native library binary files (`.so`) for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` must be compiled and packaged inside the `:feature:pdf` module, increasing the baseline APK size.

---

## 2026-06-17: Apache POI Minimization Strategy

* **Decision**: Implement a custom R8/ProGuard configuration file to aggressively optimize Apache POI.
* **Reason**: Apache POI libraries can exceed 20MB in size. Since Blink only needs reading functionality for text, layouts, and spreadsheets, we do not need chart-creation, writing, validation, or styling helper tools.
* **Impact**: Unused POI classes are stripped out during the release build. ProGuard configurations must be rigorously verified to avoid runtime `ClassNotFoundException` due to reflective class usage within Apache POI.
