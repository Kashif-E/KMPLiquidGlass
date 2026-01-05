# KMP Liquid Glass - Copilot Instructions

## Project Overview
This is **KMP Liquid Glass (Backdrop)** - a customizable Liquid Glass effect library for Jetpack Compose Multiplatform. Original author: [Kyant](https://github.com/kyant).

## Tech Stack (Non-Negotiable)
- **Language:** Kotlin (100%)
- **UI Framework:** Jetpack Compose Multiplatform (NOT XML layouts, NOT SwiftUI)
- **Build System:** Gradle with Kotlin DSL (`.gradle.kts`)
- **Minimum JDK:** 21
- **Architecture:** Compose-first, declarative UI patterns

## Supported Platforms
- Android (min SDK 21, target SDK 36)
- Desktop (JVM via Skia)
- iOS (arm64, simulatorArm64, x64 via Skia)
- Web (Wasm/JS via Skia)

## Project Structure
```
backdrop/           # Core library module (published to Maven Central)
├── commonMain/     # Shared Kotlin code for all platforms
├── androidMain/    # Android-specific implementations
├── skiaMain/       # Shared code for Skia-based platforms (iOS, Desktop, Web)
├── iosMain/        # iOS-specific code
├── desktopMain/    # Desktop JVM-specific code (depends on skiaMain)
└── wasmJsMain/     # Web-specific code (depends on skiaMain)

catalog/            # Demo/sample application
├── sharedUI/       # Shared UI components for catalog app
├── androidApp/     # Android catalog app
├── desktopApp/     # Desktop catalog app
├── webApp/         # Web catalog app
└── iosApp/         # iOS catalog app (Xcode project)
```

## Coding Standards

### Kotlin Style
- Use `val` over `var` whenever possible (immutability first)
- Prefer extension functions for utility methods
- Use meaningful parameter names in public APIs
- Document all public APIs with KDoc

### Compose Guidelines
- Use `Modifier` as the first optional parameter in composable functions
- Chain modifiers in a readable order: layout → appearance → interaction
- Prefer `remember` and `derivedStateOf` for computed values
- Use `LaunchedEffect` and `rememberCoroutineScope` for side effects

### Platform-Specific Code
- Place shared Skia code in `skiaMain` source set (iOS, Desktop, Web share this)
- Use `expect`/`actual` declarations for platform abstractions
- Android-specific code uses native Android Graphics APIs
- Skia platforms use `org.jetbrains.skia` APIs

## Forbidden Practices
- **DO NOT** use XML layouts or Android View system
- **DO NOT** mix Material 2 and Material 3 imports
- **DO NOT** use `mutableStateOf` without `remember`
- **DO NOT** create platform-specific code in `commonMain`
- **DO NOT** use deprecated Compose APIs

## Dependencies & Imports
- Use `androidx.compose.*` for Android-specific Compose code
- Use `org.jetbrains.compose.*` for multiplatform Compose code
- Serialization: Use `kotlinx.serialization` (NOT Gson)
- Graphics: Use platform-appropriate APIs (Android Graphics vs Skia)

## Library Publishing
- Published to Maven Central as `io.github.kashif-mehmood-km:backdrop`
- Uses Vanniktech Maven Publish plugin
- Versioning follows semantic versioning

## Testing
- Write unit tests in `commonTest` source set when possible
- Use `@Preview` composables for visual testing
- Test on multiple platforms before publishing
