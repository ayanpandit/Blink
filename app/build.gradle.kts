import java.net.URLClassLoader

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ayanpandey.blink"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ayanpandey.blink"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.0.8"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Project dependencies
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:file"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // Feature dependencies
    implementation(project(":feature:home"))
    implementation(project(":feature:scanner"))
    implementation(project(":feature:pdf"))
    implementation(project(":feature:word"))
    implementation(project(":feature:excel"))
    implementation(project(":feature:ppt"))
    implementation(project(":feature:text"))
    implementation(project(":feature:viewer"))

    // Libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("generateAppIcons") {
    group = "build"
    description = "Generates launcher, adaptive, and notification icons from root icon.png"
    doLast {
        val rootDir = project.rootDir
        val srcFile = File(rootDir, "icon.png")
        if (!srcFile.exists()) {
            throw GradleException("Source icon.png not found in repository root!")
        }
        val resDir = File(projectDir, "src/main/res")
        val generatorJava = File(rootDir, "tools/IconGenerator.java")
        val classesDir = File(layout.buildDirectory.get().asFile, "iconGeneratorClasses")
        classesDir.mkdirs()

        // Compile IconGenerator.java using Java Compiler API
        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            throw GradleException("System Java Compiler (javac) is not available! Make sure you are running Gradle on a JDK.")
        }
        val fileManager = compiler.getStandardFileManager(null, null, null)
        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(listOf(generatorJava))
        val options = listOf("-d", classesDir.absolutePath)
        val task = compiler.getTask(null, fileManager, null, options, null, compilationUnits)
        val success = task.call()
        fileManager.close()
        if (!success) {
            throw GradleException("Failed to compile tools/IconGenerator.java!")
        }

        // Run IconGenerator using ClassLoader
        logger.lifecycle("Running IconGenerator...")
        val classLoader = URLClassLoader(
            arrayOf(classesDir.toURI().toURL()),
            ClassLoader.getSystemClassLoader()
        )
        val mainClass = classLoader.loadClass("IconGenerator")
        val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
        mainMethod.invoke(null, arrayOf(srcFile.absolutePath, resDir.absolutePath) as Any)
        logger.lifecycle("Successfully generated all icons from root icon.png")
    }
}

// Hook it to preBuild
tasks.named("preBuild") {
    dependsOn("generateAppIcons")
}
