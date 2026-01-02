# KMP Liquid Glass (Backdrop)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kashif-mehmood-km/backdrop)](https://central.sonatype.com/artifact/io.github.kashif-mehmood-km/backdrop)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.6+-blue.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

A customizable **Liquid Glass** effect library for **Jetpack Compose Multiplatform**. Create stunning glass morphism effects with blur, refraction, highlights, and shadows across all platforms.

> 🙏 Original Android library by [Kyant](https://github.com/Kyant0/AndroidLiquidGlass). This fork extends it to support all Compose Multiplatform targets.

![KMP Liquid Glass Banner](artworks/liquid_glass_kmp.png)

## ✨ Features

- **🔮 Glass Effects** - Blur, refraction, color controls, and vibrancy
- **💡 Dynamic Highlights** - Ambient and directional lighting with customizable angles
- **🌊 Lens Refraction** - Advanced glass distortion with chromatic aberration support
- **📱 SDF Textures** - Signed Distance Field shader for text/shape refraction (Lock Screen demo)
- **🎨 Adaptive Luminance** - Glass that adapts to background brightness
- **📜 Progressive Blur** - Gradient blur effects for scroll containers
- **🖱️ Interactive** - Draggable glass elements with physics-based animations

## 🎯 Supported Platforms

| Platform | Status | Notes |
|----------|--------|-------|
| 🤖 Android | ✅ Full Support | All effects including AGSL shaders (API 33+) |
| 🖥️ Desktop (JVM) | ✅ Full Support | Skia-based rendering |
| 🍎 iOS | ✅ Full Support | Skia-based rendering |
| 🌐 Web (Wasm/JS) | ✅ Full Support | Skia-based rendering |

## 📦 Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
// For Compose Multiplatform projects
commonMain.dependencies {
    implementation("io.github.kashif-mehmood-km:backdrop:<version>")
}

// For Android-only projects
dependencies {
    implementation("io.github.kashif-mehmood-km:backdrop:<version>")
}
```

## 🚀 Quick Start

```kotlin
@Composable
fun GlassCard() {
    val backdrop = rememberLayerBackdrop()
    
    // Background layer that glass will sample from
    Image(
        painter = painterResource(Res.drawable.background),
        modifier = Modifier.layerBackdrop(backdrop)
    )
    
    // Glass element
    Box(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = {
                    colorControls(brightness = -0.1f, saturation = 1.5f)
                    blur(16.dp.toPx())
                    lens(24.dp.toPx(), 32.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                shadow = { Shadow.Default }
            )
            .size(200.dp)
    )
}
```

## 🎨 Available Effects

| Effect | Description | Platform Support |
|--------|-------------|------------------|
| `blur()` | Gaussian blur | All |
| `colorControls()` | Brightness, contrast, saturation | All |
| `vibrancy()` | iOS-style vibrancy effect | All |
| `lens()` | Glass refraction with depth | All |
| `progressiveBlur()` | Gradient blur for scroll | All |
| `opacity()` | Alpha blending | All |
| `exposureAdjustment()` | EV-based exposure | All |
| `gammaAdjustment()` | Gamma correction | All |

## 📚 Documentation

For detailed API documentation and advanced usage, visit the [Documentation](https://kyant.gitbook.io/backdrop).

## 🧩 Example Components

The library provides low-level primitives. Here are example high-level components from the catalog app:

- [LiquidButton](catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/components/LiquidButton.kt) - Animated glass button
- [LiquidToggle](catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/components/LiquidToggle.kt) - iOS-style toggle switch
- [LiquidSlider](catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/components/LiquidSlider.kt) - Glass slider control
- [LiquidBottomTabs](catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/components/LiquidBottomTabs.kt) - Tab bar with glass indicators

## 📱 Demos

### Catalog App Screenshots

![Backdrop Catalog](artworks/banner.jpg)

![Catalog Screenshots](artworks/catalog_app.jpg)

### Running the Demos

```bash
# Desktop
./gradlew :catalog:desktopApp:run

# Android
./gradlew :catalog:androidApp:installDebug

# Web
./gradlew :catalog:webApp:wasmJsBrowserRun

# iOS (requires Xcode)
open catalog/iosApp/iosApp.xcodeproj
```

## 🏗️ Project Structure

```
backdrop/                 # Core library (published to Maven Central)
├── commonMain/          # Shared Kotlin code
├── androidMain/         # Android-specific (AGSL shaders)
└── skiaMain/            # Skia-based platforms (iOS, Desktop, Web)

catalog/                  # Demo application
├── sharedUI/            # Shared UI components
├── androidApp/          # Android demo
├── desktopApp/          # Desktop demo
├── webApp/              # Web demo
└── iosApp/              # iOS demo (Xcode project)
```

## 📄 License

```
Copyright 2025 Kyant (Original), Kashif Mehmood (KMP Fork)

Licensed under the Apache License, Version 2.0
```

## 🤝 Contributing

Contributions are welcome! Please read the contribution guidelines before submitting PRs.

## 🔗 Links

- [Original Android Library](https://github.com/Kyant0/AndroidLiquidGlass)
- [Maven Central](https://central.sonatype.com/artifact/io.github.kashif-mehmood-km/backdrop)
- [Documentation](https://kyant.gitbook.io/backdrop)
