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

---

## 2026-06-17: Centralized Version Catalog for Dependencies

* **Decision**: Centralize all dependency coordinates and Gradle plugins in a Gradle Version Catalog (`libs.versions.toml`).
* **Reason**: Managing dependencies across 15 separate Gradle modules individually leads to version drift, compilation conflicts, and difficult upgrades. A centralized TOML file ensures version consistency and type-safe referencing.
* **Impact**: Individual module build files are prohibited from declaring hardcoded dependency versions. All references must resolve through `libs.*`.

---

## 2026-06-17: Unified Sealed Interface Error Model

* **Decision**: Define a single unified sealed interface `AppError` in the `:core:common` module.
* **Reason**: Each format parser generates library-specific exceptions (e.g. POI Exceptions, PDFium JNI errors, standard IOExceptions). Capturing these raw exceptions in the presentation layer leads to bloated, unmaintainable catch blocks. By mapping internal errors to a strict sealed model, we decouple the UI from format-specific exceptions.
* **Impact**: Core and feature loading layers must map all internal parse and load exceptions to the appropriate sub-types of `AppError` before returning them to ViewModels.

---

## 2026-06-18: Secure Android File System Integration via Storage Access Framework (SAF)

* **Decision**: Access local and external files using Android's Storage Access Framework (SAF) and `ContentResolver` querying display name and size parameters directly.
* **Reason**: Broad read/write storage permissions (such as `READ_EXTERNAL_STORAGE`) are heavily restricted in modern Android versions, complex to request, and introduce user trust barriers. SAF offers a secure, scoped access mechanism where users grant explicit access to single files through system interfaces or standard application intents.
* **Impact**: Incoming `content://` and `file://` URIs are handled dynamically. Metadata querying verifies mime-types and extensions against a whitelist before loading file contents. Permission security exceptions or URI resolution failures are mapped directly onto the `AppError` architecture.

---

## 2026-06-18: Persistent URI Permission Acquisition for Storage Access Framework (SAF)

* **Decision**: Explicitly request persistable URI read permissions (`takePersistableUriPermission`) during file picker selection and main activity view intent processing, using appropriate context/activity scopes, AND retrieve the active `Activity` context dynamically instead of the Application context for resolver queries and stream opens.
* **Reason**: SAF temporary permissions granted to a launched Activity do not automatically propagate to background coroutines running on `Dispatchers.IO` or when accessing the `ContentResolver` using application contexts. By taking persistable read permissions on `content://` URIs immediately upon acquisition, we prevent "Access Denied" `SecurityException` crashes during subsequent background metadata queries and file reads. Furthermore, accessing the URI via the active Activity context's ContentResolver guarantees that even if a custom document provider does not support persistable URI permissions, our app still has temporary access to read and query it during the active user session.
* **Impact**: All content URI entry points (Compose file picker callbacks and intent-based launcher starts) must call `takePersistableUriPermission` with `FLAG_GRANT_READ_URI_PERMISSION`. The file resolver tracks the current active activity via a lifecycle listener and resolves all content URIs using the active Activity's `ContentResolver`.

---

## 2026-06-18: Runtime SAF Diagnostic Logging and Debug Details Overlay

* **Decision**: Refactor `AppError.FileError.PermissionDenied` to carry throwable cause details and stack traces, and add a copyable visual diagnostic panel to the error screen displaying URI scheme, authority, checked permissions, persisted permissions list, and exception stack trace.
* **Reason**: Permission denials under Android Storage Access Framework (SAF) are highly environment-dependent and often impossible to reproduce on emulators or other devices. Standard stack traces are normally swallowed or logged only in standard logcat which is inaccessible to end users on non-development physical devices. Providing a diagnostic panel directly on-screen with complete details (including copy-paste capability) enables rapid, evidence-based debugging on real physical devices.
* **Impact**: `PermissionDenied` is changed to a data class storing failure cause and stack traces. `FileResolverImpl` catches `SecurityException` and captures the stack trace string. The `MetadataScreen` displays a detailed copyable debug card when in an error state.

---

## 2026-06-18: URL-Encoding Navigation Parameters for Content URIs

* **Decision**: All string-based file URIs passed as arguments in Jetpack Compose Navigation must be explicitly URL-encoded (using `Uri.encode(uri)`) when constructing navigation routes.
* **Reason**: Standard document URIs returned by SAF document providers contain colons (`:`) and slashes (`/`), e.g. `document/document:16585` (encoded as `document/document%3A16585` by the system). Jetpack Navigation's internal route compilation treats unencoded colons and slashes as route separators or relative schemes, corrupting the parameter value and stripping prefixes (e.g. dropping `document:` and leaving only `16585`). Encapsulating the parameter inside `Uri.encode()` protects all special characters, ensuring the destination receives the exact unmodified original URI.
* **Impact**: All route generation helper functions inside `Screen` compile routes using `Uri.encode(uri)`.

---

## 2026-06-18: Unified Document Engine Architecture with Domain-Level Contracts

* **Decision**: Define all document viewer/renderer/factory contracts (`DocumentViewer`, `DocumentRenderer`, `DocumentFactory`) in the `:domain:contract` package, with implementations in feature and core modules.
* **Reason**: Feature modules (`:feature:viewer`) depend on `:domain` but must NOT depend on `:core:file` directly. Placing `DocumentFactory` in `:core:file` created a circular dependency chain where `:feature:viewer` needed `:core:file` imports, but the architectural rule is that feature modules only depend on domain abstractions. By hosting the interface in `:domain:contract`, the implementation in `:core:file` satisfies the contract via constructor injection at the `:app` layer, maintaining clean unidirectional dependency flow.
* **Impact**: All new document-related contracts must be defined in `:domain:contract`. Implementations are wired through `AppContainerImpl` in `:app` using manual DI.

---

## 2026-06-18: Extension-First Document Type Detection Strategy

* **Decision**: `DocumentFactoryImpl` resolves `DocumentType` by checking the file extension first, falling back to MIME type only when the extension is unrecognized.
* **Reason**: Android SAF document providers sometimes return generic MIME types (e.g., `application/octet-stream`) for files with known extensions. Extension-based detection provides higher accuracy for common document formats. MIME fallback handles extensionless files shared via messaging apps.
* **Impact**: Both extension and MIME type mappings must be maintained in sync when adding new supported formats.



