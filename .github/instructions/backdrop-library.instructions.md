---
applyTo:
  - "backdrop/**"
  - "**/backdrop/**"
---

# Backdrop Library Development Skills

## Core Architecture

### Key Interfaces

**Backdrop** - Content provider interface:
```kotlin
interface Backdrop {
    val isCoordinatesDependent: Boolean
    
    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
```

**ShapeProvider** - Shape abstraction:
```kotlin
interface ShapeProvider {
    fun provideShape(): Shape
}
```

**BackdropEffectScope** - Effect configuration scope:
- Controls blur, highlight, and shadow parameters
- Provides density and layout context

### Package Structure
```
com.kashif_e.backdrop/
├── Backdrop.kt              # Core interface
├── BackdropEffectScope.kt   # Effect scope
├── DrawBackdropModifier.kt  # Main modifier (platform-specific)
├── Outline.kt               # Shape outline utilities
├── ShapeProvider.kt         # Shape provider interface
├── backdrops/               # Built-in backdrop implementations
├── effects/                 # Visual effect implementations
├── highlight/               # Highlight effect components
├── shadow/                  # Shadow effect components
└── demo/                    # Demo utilities
```

## Creating New Effects

### Effect Implementation Pattern
```kotlin
class CustomEffect : BackdropEffect {
    override fun DrawScope.drawEffect(
        scope: BackdropEffectScope,
        outline: Outline
    ) {
        // 1. Access effect parameters from scope
        val blurRadius = scope.blurRadius
        
        // 2. Draw effect using outline bounds
        drawRect(
            color = effectColor,
            topLeft = outline.bounds.topLeft,
            size = outline.bounds.size
        )
    }
}
```

### Modifier Extension Pattern
```kotlin
fun Modifier.customBackdropEffect(
    enabled: Boolean = true,
    intensity: Float = 1f
): Modifier = this.then(
    if (enabled) {
        Modifier.drawWithContent {
            // Draw backdrop content
            drawContent()
            // Apply custom effect
            drawCustomEffect(intensity)
        }
    } else {
        Modifier
    }
)
```

## Shader Development

### AGSL (Android Graphics Shading Language)
For Android 13+ (API 33):
```kotlin
private const val AGSL_SHADER = """
    uniform float2 resolution;
    uniform float intensity;
    
    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;
        // Shader logic
        return half4(color, 1.0);
    }
"""
```

### SkSL (Skia Shading Language)
For iOS, Desktop, Web:
```kotlin
private const val SKSL_SHADER = """
    uniform float2 resolution;
    uniform float intensity;
    
    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / resolution;
        // Shader logic
        return vec4(color, 1.0);
    }
"""
```

### Key Differences
| Feature | AGSL | SkSL |
|---------|------|------|
| Color type | `half4` | `vec4` |
| Coordinate param | `float2 fragCoord` | `vec2 fragCoord` |
| Return statement | `return half4(...)` | `return vec4(...)` |

## Performance Guidelines

### DO
- Cache shader instances using `remember`
- Use `derivedStateOf` for computed effect parameters
- Batch draw operations when possible
- Profile on low-end devices

### DON'T
- Create new shader instances every frame
- Allocate objects in draw loops
- Use complex effects on large surfaces without testing
- Ignore platform-specific performance characteristics

## Publishing Checklist

Before publishing a new version:
1. Update version in `backdrop/build.gradle.kts`
2. Test on all platforms (Android, iOS, Desktop, Web)
3. Update KDoc for public APIs
4. Run `./gradlew publishAllPublicationsToMavenCentralRepository`
5. Update README.md with new features

## Common Components

### LiquidButton
Interactive button with liquid glass effect and press animation.

### LiquidToggle
Toggle switch with liquid glass styling.

### LiquidSlider
Slider component with glass track and thumb.

### LiquidBottomTabs
Bottom navigation with liquid glass tab indicators.
