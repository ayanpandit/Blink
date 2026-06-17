# Changelog

All notable changes to the Blink project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.4.1-alpha] - 2026-06-18

### Fixed
- **Phase 3 Blocking Bug**: `DocumentViewerImpl.loadDocument()` used Java reflection (`getDeclaredField("fd")`) to extract a `FileDescriptor` from the InputStream returned by `ContentResolver.openInputStream()`. On Android, this returns `ParcelFileDescriptor.AutoCloseInputStream`, which does NOT have an `fd` field. This threw `NoSuchFieldException`, caught by a generic `catch` block and misreported as `AppError.FileError.CorruptedUri`. Since `DocumentFactory` only uses metadata fields (name, MIME, size) and not the FileDescriptor, the entire reflection/InputStream flow was removed. The factory now receives a dummy `FileDescriptor()`.

### Changed
- Upgraded app versioning to `versionCode = 6` and `versionName = "1.0.5"`.
- Removed unnecessary `openInputStream()` call from `DocumentViewerImpl.loadDocument()` — document model creation only requires metadata, not file content.
- Enhanced `ViewerScreen` error display to show error type, details, and received URI for on-device debugging.
- Added comprehensive diagnostic logging in `BlinkNavHost` (Metadata→Viewer navigation) and `DocumentViewerImpl` (each loading step).

## [0.4.0-alpha] - 2026-06-18

### Added
- Core document engine architecture framework (Phase 3).
- `DocumentType` enum in `:domain` mapping all 9 supported file categories (PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, CSV).
- `Document` data model representing a renderer-independent opened document.
- `DocumentState` sealed interface (`Idle`, `Loading`, `Ready`, `Error`) for viewer lifecycle state machine.
- `DocumentViewer` interface defining format-agnostic viewer contract with `StateFlow<DocumentState>`.
- `DocumentRenderer` interface defining base renderer contract with `supportedType` and `name`.
- `DocumentFactory` interface in `:domain:contract` for mapping file attributes to `Document` models.
- `DocumentFactoryImpl` in `:core:file` implementing type detection via extension-first, MIME-fallback strategy and whitelist validation.
- `DocumentError` sealed interface extending `AppError` with `UnsupportedDocument`, `CorruptedDocument`, `DocumentNotFound`, `DocumentPermissionDenied`, and `DocumentParsing` subtypes.
- New `:feature:viewer` Compose module with `DocumentViewerImpl`, `ViewerViewModel`, and placeholder `ViewerScreen`.
- `Viewer` navigation route in `:core:navigation` with URL-encoded URI parameter.
- "Open in Viewer" button on `MetadataScreen` navigating to the generic viewer placeholder.
- Unit test suites: `DocumentFactoryImplTest` (3 tests) and `DocumentViewerImplTest` (5 tests).
- `kotlinx-coroutines-test` dependency added to version catalog.

### Changed
- Upgraded app versioning to `versionCode = 5` and `versionName = "1.0.4"`.
- Moved `DocumentFactory` interface from `:core:file` to `:domain:contract` to avoid circular module dependencies.

## [0.3.2-alpha] - 2026-06-18

### Fixed
- Jetpack Navigation route argument parsing bug where colons in SAF content URIs (e.g. `document:16585`) were parsed incorrectly, resulting in prefix truncation (e.g. `16585`). All routes now explicitly URL-encode URI parameters via `Uri.encode()`, ensuring exact URI strings are preserved.

## [0.3.1-alpha] - 2026-06-18

### Added
- Detailed diagnostic logging for Storage Access Framework (SAF) URI resolution and permission checks.
- Copyable debugging information panel on error/access denied screens, capturing selected URI, authority, scheme, granted permissions, persisted permissions, and complete Exception stack traces.

### Changed
- Refactored `PermissionDenied` error representation into a data class to carry detailed failure context and stack traces.
- Upgraded app versioning configuration to `versionCode = 3` and `versionName = "1.0.2"`.

## [0.3.0-alpha] - 2026-06-18

### Added
- Secure Android file system integration using Storage Access Framework (SAF).
- Domain-level `DocumentMetadata` data model and `FileResolver` interface.
- Concrete `FileResolverImpl` in `:core:file` querying display name, file size, last modified timestamp, and resolving whitelisted MIME types/extensions.
- Manual DI integration through `DomainContainer` interface in `:domain` and `AppContainerImpl` in `:app` to resolve dependencies cleanly.
- `MainActivity` integration capturing incoming file intents (`ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`) and passing resolved URIs to the navigation layer.
- `AndroidManifest.xml` intent-filters registered for PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, CSV, and TXT files.
- `HomeScreen` document picker integration using Compose SAF `ActivityResultContracts.OpenDocument` launcher.
- Verification UI `MetadataScreen` and `MetadataViewModel` in `:feature:home` displaying resolved file parameters.
- Unit test suite `FileResolverImplTest` utilizing MockK to verify content and file schemes, permission security exceptions, and file type validation.

### Changed
- Refactored `BlinkApplication` to type cast the manual service locator as `DomainContainer` to prevent core-common circular module dependencies.
- Added `androidx.activity.compose` dependency to `:feature:home` module for contract launcher access.

### Fixed
- Android Storage Access Framework (SAF) permission handling to resolve the "Access Denied Error" when opening documents via the File Picker. Persistable read permissions are explicitly taken early, and the `FileResolver` dynamically queries files using the active `Activity` context resolver to properly honor scoped temporary URI permissions.



## [0.2.0-alpha] - 2026-06-17

### Added
- Complete 15-module multi-project Gradle project configuration: `:app`, `:core:common`, `:core:ui`, `:core:designsystem`, `:core:navigation`, `:core:file`, `:domain`, `:data`, `:feature:home`, `:feature:scanner`, `:feature:pdf`, `:feature:word`, `:feature:excel`, `:feature:ppt`, `:feature:text`.
- Centralized dependency version catalog at `gradle/libs.versions.toml`.
- Static analysis checks using `ktlint` and `detekt` configured for all subprojects, along with `.editorconfig` formatting rules.
- Custom logging abstraction `BlinkLogger` with `DebugLogger` and `ReleaseLogger` implementations.
- App-wide error handling model `AppError` representing `FileError`, `ParsingError`, and `UnknownError` states.
- Manual DI `AppContainer` infrastructure initialized lazily.
- Material 3 design system theme colors (Light and Dark schemes), typography, and grid spacing dimensions.
- Navigation routes and screen placeholders mapping all features.
- Application startup sequence registering `BlinkApplication` and launcher `MainActivity`.
- Proguard rules for production release builds with R8 code optimization.
- GitHub Actions CI workflow in `.github/workflows/android.yml` to compile and test code.

### Changed
- Configured KtLint function naming rules inside `.editorconfig` to support PascalCase for `@Composable` functions.
- Updated Detekt configuration rules for Complexity and Naming constraints.

## [0.1.0-alpha] - 2026-06-17

### Added
- Complete set of Phase 0 architectural documentation:
  - `PRODUCT_VISION.md`: Explains core mission, philosophy, and user goals.
  - `PROJECT_SCOPE.md`: Explicit list of supported formats and out-of-scope items.
  - `ARCHITECTURE.md`: Technical modular clean architecture layers, data flows, and drawing mechanisms.
  - `TECH_STACK.md`: Technical decisions for Kotlin, Jetpack Compose, Coroutines, Flow, PDFium, POI, and Coil.
  - `PERFORMANCE_TARGETS.md`: Measurable criteria for startup time (<500ms), opening (<300ms), and heap.
  - `CODING_STANDARDS.md`: Developer conventions, Compose recomposition optimization, and native cleanup.
  - `PROJECT_STRUCTURE.md`: Conceptual map of multi-project Gradle files and source packages.
  - `DECISION_LOG.md`: Rationale behind DI framework omission, module splitting, and SQLite removal.
  - `DEVELOPMENT_ROADMAP.md`: Description of milestones from Phase 0 to Phase 5.
  - `PHASE_STATUS.md`: Operational dashboard tracking active, completed, and pending phases.
  - `CHANGELOG.md`: Standard semantic log of modifications.
