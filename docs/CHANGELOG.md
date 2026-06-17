# Changelog

All notable changes to the Blink project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

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
