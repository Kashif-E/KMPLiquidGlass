---
applyTo:
  - "**/*.gradle.kts"
  - "**/gradle.properties"
  - "**/libs.versions.toml"
---

# Gradle Build System Skills

## Build Configuration

### Version Catalog (libs.versions.toml)
All dependencies are managed in `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.1"
kotlin = "2.3.0"
compose-multiplatform = "1.9.3"

[libraries]
library-name = { group = "group.id", name = "artifact", version.ref = "version-key" }

[plugins]
plugin-name = { id = "plugin.id", version.ref = "version-key" }
```

### Using Catalog in build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

dependencies {
    implementation(libs.androidx.compose.foundation)
}
```

## Multiplatform Module Configuration

### Standard KMP Module Setup
```kotlin
kotlin {
    // Android target
    androidTarget()
    
    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    
    // Desktop JVM
    jvm("desktop")
    
    // Web (Wasm)
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    // JVM toolchain
    jvmToolchain(21)
    
    // Compiler options
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xexpect-actual-classes"
        )
    }
}
```

### Source Set Dependencies
```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
    
    // Shared Skia source set
    val skiaMain by creating {
        dependsOn(commonMain)
    }
    
    val androidMain by getting {
        dependencies {
            implementation(libs.androidx.compose.foundation)
        }
    }
    
    // iOS targets depend on skiaMain
    val iosX64Main by getting { dependsOn(skiaMain) }
    val iosArm64Main by getting { dependsOn(skiaMain) }
    val iosSimulatorArm64Main by getting { dependsOn(skiaMain) }
    
    val desktopMain by getting { dependsOn(skiaMain) }
    val wasmJsMain by getting { dependsOn(skiaMain) }
}
```

## Android Configuration

```kotlin
android {
    namespace = "com.kashif_e.backdrop"
    compileSdk = 36
    buildToolsVersion = "36.1.0"
    
    defaultConfig {
        minSdk = 21
    }
    
    buildFeatures {
        compose = true
    }
}
```

## Maven Publishing

### Publishing Configuration
```kotlin
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    
    coordinates(
        groupId = "io.github.kashif-mehmood-km",
        artifactId = "backdrop",
        version = "x.y.z"
    )
    
    pom {
        name.set("KMP Liquid Glass")
        description.set("Liquid Glass effect library for Compose Multiplatform")
        // ... other POM details
    }
}
```

## Common Gradle Commands

```bash
# Build all platforms
./gradlew build

# Run Android app
./gradlew :catalog:androidApp:installDebug

# Run Desktop app
./gradlew :catalog:desktopApp:run

# Run Web app
./gradlew :catalog:webApp:wasmJsBrowserRun

# Publish to Maven Central
./gradlew publishAllPublicationsToMavenCentralRepository

# Clean build
./gradlew clean
```

## Troubleshooting

### Common Issues

**Kotlin version mismatch:**
Ensure all Kotlin-related plugins use the same version from version catalog.

**Compose version conflicts:**
Use Compose Multiplatform version for shared code, AndroidX Compose for Android-specific.

**iOS build failures:**
Check that Xcode command line tools are installed and up to date.

**Web/Wasm issues:**
Ensure Node.js is installed for web builds.
