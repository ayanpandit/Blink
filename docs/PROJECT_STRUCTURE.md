# Blink Project Structure

This document describes the proposed folder structure and modular architecture configuration of the Blink repository. No source code should exist outside of these defined directories.

---

## High-Level Repository Layout

```
Blink/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                # Root build configuration
├── settings.gradle.kts              # Module registration file
├── gradle.properties                # Global Gradle settings
├── gradlew                          # Gradle execution script
├── gradlew.bat                      # Windows Gradle execution script
│
├── docs/                           # Architectural and Product documentation
│   ├── ARCHITECTURE.md
│   ├── CHANGELOG.md
│   ├── CODING_STANDARDS.md
│   ├── DECISION_LOG.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── PERFORMANCE_TARGETS.md
│   ├── PHASE_STATUS.md
│   ├── PRODUCT_VISION.md
│   ├── PROJECT_SCOPE.md
│   ├── PROJECT_STRUCTURE.md
│   └── TECH_STACK.md
│
├── gradle/
│   └── libs.versions.toml           # Version Catalog for dependencies
│
├── app/                             # Application module (combines features)
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/blink/
│           │   ├── BlinkApplication.kt
│           │   ├── di/              # Manual DI initialization container
│           │   └── presentation/    # MainActivity and document selection view
│           └── res/
│
├── core/                            # Core infrastructure components
│   ├── common/                      # Reusable utilities and coroutines scope configurations
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/blink/core/common/
│   │
│   ├── designsystem/                # UI Theme, colors, generic widgets
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/blink/core/designsystem/
│   │
│   └── document/                    # Base abstractions, page models, parser contracts
│       ├── build.gradle.kts
│       └── src/main/java/com/blink/core/document/
│
└── feature/                         # Format-specific viewer features
    ├── pdf/                         # PDF viewer JNI and UI component implementation
    │   ├── build.gradle.kts
    │   └── src/main/java/com/blink/feature/pdf/
    │
    ├── office/                      # Microsoft Office parser and rendering components
    │   ├── build.gradle.kts
    │   └── src/main/java/com/blink/feature/office/
    │
    └── text/                        # CSV and TXT parsing layout/rendering components
        ├── build.gradle.kts
        └── src/main/java/com/blink/feature/text/
```

---

## Detailed Directory Details (Per-Module Structure)

Every Android Gradle library module (under `core/` and `feature/`) follows this standard structure:

```
[module-name]/
├── build.gradle.kts                # Module-specific dependencies and build configs
├── proguard-rules.pro              # Proguard optimization rules for the module
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/blink/[module-package]/
    │   │   ├── data/               # Implementations of data parsing & loaders
    │   │   ├── domain/             # Entities, Models, Interfaces, Use cases
    │   │   └── presentation/       # UI States, Composable files, ViewModels
    │   └── res/                    # Drawables, layouts (if any), values, strings
    │
    └── test/                       # Unit tests
        └── java/com/blink/[module-package]/
```

---

## Gradle Build Architecture

* **`settings.gradle.kts`**: Contains references to build configurations:
  ```kotlin
  include(":app")
  include(":core:common")
  include(":core:designsystem")
  include(":core:document")
  include(":feature:pdf")
  include(":feature:office")
  include(":feature:text")
  ```
* **Version Catalog (`gradle/libs.versions.toml`)**: Centralizes definitions of Android Gradle Plugin, Kotlin, Compose Compiler, Coroutines, Flow, PDFium, Apache POI, and Coil libraries to ensure uniform versioning across all modules.
