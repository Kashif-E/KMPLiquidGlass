# KMP Liquid Glass

A customizable liquid glass effect library for Compose Multiplatform. Create frosted glass, blur effects, refractions, and highlights across Android, iOS, Desktop, and Web.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kashif-mehmood-km/backdrop)](https://central.sonatype.com/artifact/io.github.kashif-mehmood-km/backdrop)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.6+-blue.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

> Based on [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) by Kyant. This fork adds Compose Multiplatform support.

![Banner](artworks/liquid_glass_kmp.png)

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
  - [Backdrops](#backdrops)
  - [Effects](#effects)
  - [Highlights](#highlights)
  - [Shadows](#shadows)
- [API Reference](#api-reference)
  - [drawBackdrop](#drawbackdrop)
  - [drawPlainBackdrop](#drawplainbackdrop)
  - [Effect Functions](#effect-functions)
- [Examples](#examples)
  - [Basic Glass Card](#basic-glass-card)
  - [Frosted Glass with Blur](#frosted-glass-with-blur)
  - [Glass Button](#glass-button)
  - [Glass Dialog](#glass-dialog)
  - [Draggable Glass Element](#draggable-glass-element)
  - [Adaptive Glass](#adaptive-glass)
- [Platform Support](#platform-support)
- [Project Structure](#project-structure)
- [Running the Demos](#running-the-demos)
- [License](#license)

---

## Installation

Add the dependency to your project:

**Kotlin DSL (Multiplatform)**

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("io.github.kashif-mehmood-km:backdrop:1.0.4")
}
```

**Kotlin DSL (Android only)**

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.kashif-mehmood-km:backdrop:1.0.4")
}
```

**Version Catalog**

```toml
# libs.versions.toml
[versions]
backdrop = "1.0.4"

[libraries]
backdrop = { module = "io.github.kashif-mehmood-km:backdrop", version.ref = "backdrop" }
```

---

## Quick Start

Here's a minimal example to create a glass effect:

```kotlin
@Composable
fun GlassExample() {
    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        // Step 1: Define the content that glass will sample from
        Image(
            painter = painterResource(Res.drawable.background),
            contentDescription = null,
            modifier = Modifier
                .layerBackdrop(backdrop)  // Capture this layer
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Step 2: Create a glass element that samples from the backdrop
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        blur(16.dp.toPx())
                    }
                )
                .size(200.dp)
        )
    }
}
```

---

## Core Concepts

### Backdrops

A **backdrop** is the content that glass effects sample from. The library provides several backdrop types:

| Type | Description | Use Case |
| ---- | ----------- | -------- |
| `LayerBackdrop` | Captures rendered content from a composable | Most common - sample from images, gradients |
| `CanvasBackdrop` | Custom canvas drawing | Procedural backgrounds |
| `CombinedBackdrop` | Combines multiple backdrops | Complex layered effects |

**Creating a LayerBackdrop:**

```kotlin
val backdrop = rememberLayerBackdrop()

// Capture content into the backdrop
Image(
    painter = painterResource(Res.drawable.photo),
    modifier = Modifier.layerBackdrop(backdrop)
)

// Use the captured content as glass background
Box(
    modifier = Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { CircleShape },
        effects = { blur(12.dp.toPx()) }
    )
)
```

**Creating a CanvasBackdrop:**

```kotlin
val backdrop = rememberCanvasBackdrop {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Blue, Color.Purple)
        )
    )
}
```

### Effects

Effects transform the sampled backdrop content. Chain multiple effects for complex looks:

```kotlin
effects = {
    // Order matters - effects are applied in sequence
    colorControls(brightness = -0.1f, saturation = 1.5f)
    blur(16.dp.toPx())
    lens(24.dp.toPx(), 32.dp.toPx())
}
```

### Highlights

Highlights add glossy reflections to glass edges:

```kotlin
// Preset highlights
highlight = { Highlight.Default }   // Gradient-based
highlight = { Highlight.Ambient }   // Environmental lighting
highlight = { Highlight.Plain }     // Simple solid

// Custom highlight
highlight = {
    Highlight(
        width = 1.dp,
        blurRadius = 0.5.dp,
        alpha = 0.8f,
        style = HighlightStyle.Default
    )
}
```

### Shadows

Add depth with drop shadows and inner shadows:

```kotlin
// Drop shadow
shadow = {
    Shadow(
        radius = 24.dp,
        offset = DpOffset(0.dp, 4.dp),
        color = Color.Black.copy(alpha = 0.1f)
    )
}

// Inner shadow for pressed states
innerShadow = {
    InnerShadow(
        radius = 4.dp,
        alpha = 0.5f
    )
}
```

---

## API Reference

### drawBackdrop

The primary modifier for creating glass effects with all features.

```kotlin
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = null,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier
```

**Parameters:**

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `backdrop` | `Backdrop` | Source content for the glass effect |
| `shape` | `() -> Shape` | Shape of the glass area |
| `effects` | `BackdropEffectScope.() -> Unit` | Effect chain (blur, color, lens) |
| `highlight` | `(() -> Highlight?)?` | Optional edge highlight |
| `shadow` | `(() -> Shadow?)?` | Optional drop shadow |
| `innerShadow` | `(() -> InnerShadow?)?` | Optional inner shadow |
| `layerBlock` | `(GraphicsLayerScope.() -> Unit)?` | Graphics layer transforms |
| `exportedBackdrop` | `LayerBackdrop?` | Export this glass as backdrop for others |
| `onDrawBehind` | `(DrawScope.() -> Unit)?` | Draw behind the backdrop |
| `onDrawBackdrop` | Custom backdrop drawing | Intercept and customize backdrop draw |
| `onDrawSurface` | `(DrawScope.() -> Unit)?` | Draw on the glass surface |
| `onDrawFront` | `(DrawScope.() -> Unit)?` | Draw in front of everything |

### drawPlainBackdrop

Simplified modifier without shadows or highlights.

```kotlin
fun Modifier.drawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier
```

### Effect Functions

All effects are called within the `BackdropEffectScope`:

#### blur

```kotlin
fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode = TileMode.Clamp
)
```

Applies gaussian blur. Radius is in pixels - use `Dp.toPx()` for density-independent values.

```kotlin
effects = {
    blur(16.dp.toPx())
}
```

#### colorControls

```kotlin
fun BackdropEffectScope.colorControls(
    brightness: Float = 0f,      // -1.0 to 1.0
    contrast: Float = 1f,        // 0.0 to 2.0+
    saturation: Float = 1f       // 0.0 to 2.0+
)
```

Adjusts color properties of the backdrop.

```kotlin
effects = {
    // Slightly darker, more vibrant
    colorControls(brightness = -0.1f, contrast = 1.2f, saturation = 1.5f)
}
```

#### vibrancy

```kotlin
fun BackdropEffectScope.vibrancy()
```

Applies an iOS-style vibrancy effect that increases saturation.

```kotlin
effects = {
    vibrancy()
    blur(8.dp.toPx())
}
```

#### lens

```kotlin
fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
)
```

Creates a glass lens refraction effect. Simulates light bending through curved glass.

```kotlin
effects = {
    lens(
        refractionHeight = 24.dp.toPx(),
        refractionAmount = 32.dp.toPx(),
        depthEffect = true,           // Adds depth shading
        chromaticAberration = true    // Adds color fringing
    )
}
```

#### opacity

```kotlin
fun BackdropEffectScope.opacity(alpha: Float)
```

Adjusts the opacity of the backdrop content.

```kotlin
effects = {
    opacity(0.8f)
    blur(12.dp.toPx())
}
```

#### exposureAdjustment

```kotlin
fun BackdropEffectScope.exposureAdjustment(ev: Float)
```

Adjusts exposure using EV (exposure value) units.

```kotlin
effects = {
    exposureAdjustment(0.5f)  // Slightly brighter
}
```

#### gammaAdjustment

```kotlin
fun BackdropEffectScope.gammaAdjustment(power: Float)
```

Applies gamma correction.

```kotlin
effects = {
    gammaAdjustment(1.2f)
}
```

#### progressiveBlur

```kotlin
fun BackdropEffectScope.progressiveBlur(
    startRadius: Float,
    endRadius: Float,
    direction: ProgressiveBlurDirection = ProgressiveBlurDirection.TopToBottom
)
```

Creates a gradient blur effect, useful for scroll containers.

```kotlin
effects = {
    progressiveBlur(
        startRadius = 0f,
        endRadius = 16.dp.toPx(),
        direction = ProgressiveBlurDirection.TopToBottom
    )
}
```

---

## Examples

### Basic Glass Card

A simple frosted glass card:

```kotlin
@Composable
fun GlassCard(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    colorControls(brightness = 0.1f, saturation = 1.2f)
                    blur(20.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                shadow = { Shadow.Default },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .padding(16.dp)
    ) {
        content()
    }
}
```

### Frosted Glass with Blur

Creating an iOS-style frosted glass effect:

```kotlin
@Composable
fun FrostedGlass(backdrop: LayerBackdrop) {
    Box(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = {
                    // iOS-style frosted glass recipe
                    vibrancy()
                    colorControls(brightness = 0.05f, saturation = 1.3f)
                    blur(24.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = {
                    // Semi-transparent overlay
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .size(300.dp, 200.dp)
    )
}
```

### Glass Button

An interactive glass button with press states:

```kotlin
@Composable
fun GlassButton(
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(12.dp) },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    if (isPressed) {
                        colorControls(brightness = -0.1f)
                    }
                },
                highlight = {
                    if (isPressed) Highlight.Plain.copy(alpha = 0.5f)
                    else Highlight.Ambient
                },
                shadow = {
                    Shadow(
                        radius = if (isPressed) 4.dp else 12.dp,
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                },
                innerShadow = {
                    if (isPressed) InnerShadow(radius = 2.dp, alpha = 0.3f)
                    else null
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = if (isPressed) 0.05f else 0.1f))
                }
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// Usage
GlassButton(
    onClick = { /* handle click */ },
    backdrop = backdrop
) {
    Text("Click Me", color = Color.White)
}
```

### Glass Dialog

A modal dialog with glass background:

```kotlin
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) { } // Prevent dismiss on content click
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = {
                        colorControls(brightness = 0.1f, saturation = 1.5f)
                        blur(24.dp.toPx())
                        lens(16.dp.toPx(), 24.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    shadow = {
                        Shadow(
                            radius = 32.dp,
                            offset = DpOffset(0.dp, 8.dp),
                            color = Color.Black.copy(alpha = 0.15f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.2f))
                    }
                )
                .padding(24.dp)
                .widthIn(max = 400.dp)
        ) {
            content()
        }
    }
}
```

### Draggable Glass Element

A glass element that can be dragged around:

```kotlin
@Composable
fun DraggableGlass(backdrop: LayerBackdrop) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
            }
            .draggable2D(
                state = rememberDraggable2DState { delta ->
                    offset += delta
                }
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(20.dp) },
                effects = {
                    colorControls(brightness = -0.05f, contrast = 0.9f, saturation = 1.4f)
                    blur(16.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow.Default },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .size(150.dp, 100.dp)
    )
}
```

### Adaptive Glass

Glass that adapts to background brightness:

```kotlin
@Composable
fun AdaptiveGlass(
    backdrop: LayerBackdrop,
    isLightBackground: Boolean
) {
    val surfaceColor = if (isLightBackground) {
        Color.White.copy(alpha = 0.2f)
    } else {
        Color.Black.copy(alpha = 0.2f)
    }

    val contentColor = if (isLightBackground) Color.Black else Color.White

    Box(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    if (isLightBackground) {
                        colorControls(brightness = 0.1f, saturation = 1.2f)
                        blur(20.dp.toPx())
                    } else {
                        colorControls(brightness = -0.1f, saturation = 1.4f)
                        blur(16.dp.toPx())
                    }
                },
                highlight = {
                    if (isLightBackground) Highlight.Ambient
                    else Highlight.Plain.copy(alpha = 0.3f)
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                }
            )
            .padding(16.dp)
    ) {
        Text("Adaptive Content", color = contentColor)
    }
}
```

---

## Platform Support

| Platform | Minimum Version | Rendering | Notes |
| -------- | --------------- | --------- | ----- |
| Android | API 21 (Lollipop) | Native / AGSL | Full effects on API 33+, basic blur on older |
| iOS | iOS 13+ | Skia | Full support via Compose Multiplatform |
| Desktop | JDK 11+ | Skia | Full support on Windows, macOS, Linux |
| Web | Modern browsers | Skia/Canvas | Wasm and JS targets supported |

### Platform-Specific Notes

**Android:** Advanced shader effects (lens refraction, chromatic aberration) require Android 13 (API 33) or higher. On older versions, the library gracefully falls back to basic blur effects.

**Skia Platforms (iOS, Desktop, Web):** Use SkSL shaders which provide equivalent functionality to Android's AGSL shaders.

---

## Project Structure

```text
backdrop/                    # Core library module
├── commonMain/             # Shared Kotlin code (expect declarations)
├── androidMain/            # Android implementation (AGSL shaders)
└── skiaMain/               # Skia implementation (iOS, Desktop, Web)

catalog/                     # Demo application
├── sharedUI/               # Shared demo components
│   ├── commonMain/         # Cross-platform UI
│   ├── androidMain/        # Android-specific demos
│   └── skiaMain/           # Skia-specific demos
├── androidApp/             # Android demo app
├── desktopApp/             # Desktop demo app
├── webApp/                 # Web demo app
└── iosApp/                 # iOS demo app (Xcode project)
```

---

## Running the Demos

Clone the repository and run the demo apps:

```bash
# Clone
git clone https://github.com/Kashif-E/KMPLiquidGlass.git
cd KMPLiquidGlass

# Desktop
./gradlew :catalog:desktopApp:run

# Android (requires connected device or emulator)
./gradlew :catalog:androidApp:installDebug

# Web
./gradlew :catalog:webApp:wasmJsBrowserRun

# iOS (requires macOS with Xcode)
open catalog/iosApp/iosApp.xcodeproj
# Then build and run from Xcode
```

---

## License

```text
Copyright 2025 Kyant (Original), Kashif Mehmood (KMP Fork)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Links

- [Original Android Library](https://github.com/Kyant0/AndroidLiquidGlass)
- [Maven Central](https://central.sonatype.com/artifact/io.github.kashif-mehmood-km/backdrop)
- [API Documentation](https://kyant.gitbook.io/backdrop)
