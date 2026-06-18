pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://andob.io/repository/open_source") }
    }
}

rootProject.name = "Blink"

// Include Application module
include(":app")

// Include Core modules
include(":core:common")
include(":core:ui")
include(":core:designsystem")
include(":core:navigation")
include(":core:file")

// Include Domain and Data modules
include(":domain")
include(":data")

// Include Feature modules
include(":feature:home")
include(":feature:scanner")
include(":feature:pdf")
include(":feature:word")
include(":feature:excel")
include(":feature:ppt")
include(":feature:text")
include(":feature:viewer")

