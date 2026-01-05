---
applyTo:
  - "**/commonMain/**"
  - "**/androidMain/**"
  - "**/iosMain/**"
  - "**/desktopMain/**"
  - "**/skiaMain/**"
  - "**/wasmJsMain/**"
---

# Kotlin Multiplatform Development Skills

## Source Set Hierarchy

This project uses a custom source set hierarchy:

```
commonMain
├── androidMain (Android-specific, uses Android Graphics APIs)
└── skiaMain (Shared for all Skia-based platforms)
    ├── iosX64Main
    ├── iosArm64Main
    ├── iosSimulatorArm64Main
    ├── desktopMain (JVM)
    └── wasmJsMain (Web)
```

## Expect/Actual Pattern

### When to Use
- Platform-specific rendering implementations
- Native API access (file system, sensors, etc.)
- Performance-critical code paths

### Declaration Pattern
```kotlin
// In commonMain
expect fun platformSpecificFunction(): Result

// In androidMain
actual fun platformSpecificFunction(): Result {
    // Android implementation using Android APIs
}

// In skiaMain (covers iOS, Desktop, Web)
actual fun platformSpecificFunction(): Result {
    // Skia-based implementation
}
```

## Platform-Specific Imports

### Android (androidMain)
```kotlin
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
```

### Skia Platforms (skiaMain)
```kotlin
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
```

## Graphics Implementation Differences

### Blur Effects

**Android:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    RenderEffect.createBlurEffect(
        radiusX, radiusY,
        Shader.TileMode.CLAMP
    ).asComposeRenderEffect()
}
```

**Skia (iOS/Desktop/Web):**
```kotlin
ImageFilter.makeBlur(
    sigmaX = radius,
    sigmaY = radius,
    mode = FilterBlurMode.CLAMP
)
```

### Shader Effects

**Android:**
```kotlin
RuntimeShader(AGSL_SHADER_CODE).apply {
    setFloatUniform("uniformName", value)
}
```

**Skia:**
```kotlin
RuntimeEffect.makeForShader(SKSL_SHADER_CODE)?.let { effect ->
    RuntimeShaderBuilder(effect).apply {
        uniform("uniformName", value)
    }
}
```

## Build Configuration

### Compiler Options
The project uses these Kotlin compiler options:
- `-Xcontext-parameters` - Enable context parameters
- `-Xexpect-actual-classes` - Enable expect/actual for classes

### JVM Toolchain
Target JDK 21 for all JVM targets (Android, Desktop)

## Common Pitfalls to Avoid

### ❌ Don't Do
```kotlin
// Wrong: Platform-specific code in commonMain
import android.graphics.Bitmap  // Won't compile on other platforms

// Wrong: Skia imports in androidMain
import org.jetbrains.skia.Image  // Android uses Android Graphics
```

### ✅ Do Instead
```kotlin
// Correct: Use expect/actual for platform differences
expect class PlatformImage

// In androidMain
actual typealias PlatformImage = android.graphics.Bitmap

// In skiaMain
actual typealias PlatformImage = org.jetbrains.skia.Image
```

## Testing Across Platforms

- Write tests in `commonTest` for shared logic
- Use `androidTest` for Android-specific integration tests
- Test visual output on each platform before releases
