---
applyTo:
  - "**/*.kt"
  - "**/commonMain/**"
  - "**/androidMain/**"
---

# Compose UI Development Skills

## Composable Function Standards

### Function Signature Pattern
```kotlin
@Composable
fun ComponentName(
    // Required parameters first
    requiredParam: Type,
    // Modifier always second (with default)
    modifier: Modifier = Modifier,
    // Optional parameters with defaults
    optionalParam: Type = defaultValue,
    // Content lambda last (if applicable)
    content: @Composable () -> Unit = {}
)
```

### Preview Generation
Always generate preview composables for UI components:
```kotlin
@Preview
@Composable
private fun ComponentNamePreview() {
    // Wrap in theme if available
    ComponentName(
        requiredParam = sampleValue
    )
}
```

## Backdrop Library Specific

### Effect Modifiers
When working with backdrop effects, use the provided modifier extensions:
- `Modifier.drawBackdrop()` - Main entry point for backdrop effects
- `Modifier.liquidGlass()` - Applies liquid glass visual effect
- `Modifier.backdropBlur()` - Applies blur to backdrop content

### Shape Providers
Use `ShapeProvider` interface for custom shapes:
```kotlin
val customShape = object : ShapeProvider {
    override fun provideShape(): Shape = RoundedCornerShape(16.dp)
}
```

### Effect Scope
Access `BackdropEffectScope` for advanced effect customization:
- Configure blur radius
- Adjust highlight intensity
- Control shadow parameters

## State Management

### Correct Patterns
```kotlin
// ✅ Good: State with remember
var expanded by remember { mutableStateOf(false) }

// ✅ Good: Derived state for computed values
val alpha by remember { derivedStateOf { if (expanded) 1f else 0.5f } }

// ✅ Good: Animation state
val animatedAlpha by animateFloatAsState(targetValue = alpha)
```

### Incorrect Patterns
```kotlin
// ❌ Bad: State without remember (resets on recomposition)
var expanded by mutableStateOf(false)

// ❌ Bad: Expensive computation without derivedStateOf
val computed = expensiveCalculation(state)
```

## Graphics & Drawing

### Custom Drawing
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // Use drawScope extensions
    drawRoundRect(
        color = Color.White,
        cornerRadius = CornerRadius(16.dp.toPx())
    )
}
```

### Layer Effects
```kotlin
Modifier.graphicsLayer {
    // Clip and transform
    clip = true
    shape = RoundedCornerShape(16.dp)
    
    // Blur (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        renderEffect = RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
    }
}
```

## Accessibility
- All interactive elements must have `contentDescription`
- Use `semantics` modifier for screen reader support
- Ensure sufficient color contrast for text
