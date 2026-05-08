# Platform support & API levels

The library is Compose Multiplatform, so one source produces working glass on Android, iOS, Desktop (JVM), and Web. Platforms differ in *how* the GPU work happens; effect availability is uniform with one Android API-level caveat.

## Compatibility matrix

| Effect | Android 31–32 | Android 33+ | iOS | Desktop | Web |
|---|---|---|---|---|---|
| `blur` | ✅ Native `RenderEffect` | ✅ | ✅ Skia | ✅ Skia | ✅ Skia |
| `colorControls` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `progressiveBlur` | ⚠️ degrades to flat blur | ✅ AGSL | ✅ SkSL | ✅ | ✅ |
| `opacity` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `vibrancy` | ❌ no-op | ✅ AGSL | ✅ | ✅ | ✅ |
| `lens` | ❌ no-op | ✅ AGSL | ✅ | ✅ | ✅ |
| `exposureAdjustment` / `gammaAdjustment` | ❌ no-op | ✅ | ✅ | ✅ | ✅ |
| `reflectiveGlass` | ❌ no-op | ✅ | ✅ | ✅ | ✅ |
| SDF shader | ❌ no-op | ✅ | ✅ | ✅ | ✅ |
| Highlight / Shadow / InnerShadow | ✅ | ✅ | ✅ | ✅ | ✅ |
| Custom `RuntimeShader` | ❌ | ✅ | ✅ via `RuntimeEffect` | ✅ | ✅ |

"No-op" means the call is silently ignored — the surrounding `drawBackdrop` still draws (with whatever effects *did* run), so your component shows up; it just doesn't have liquid refraction on Android < 33.

## What this means for your component

You almost never need explicit version branches. The library degrades gracefully: a glass card on Android 32 still gets `blur`, the highlight, and the shadow — it just lacks the lens refraction. The result looks like classic frosted glass, which is fine.

If you specifically need the design to compensate (e.g. a heavier blur or a lighter surface tint) on older Android, gate at the call site:

```kotlin
val isAgsl = !isAndroid || androidApiLevel() >= 33
val highBlur = if (isAgsl) 8.dp else 12.dp
```

The library does **not** try to fake `lens` with extra blur on older APIs — that's left to you. Most apps don't bother.

## Rendering pipelines

| Platform | Effect engine | Shader language |
|---|---|---|
| Android 31–32 | `android.graphics.RenderEffect` | (no shaders) |
| Android 33+ | `RenderEffect` + `RuntimeShader` | AGSL |
| iOS / Desktop / Web | Skia `ImageFilter` + `RuntimeEffect` | SkSL |

The library hides this — you write the same Compose code on all five platforms. Only `expect`/`actual` glue inside the library cares.

## When to write platform-specific shader code

Almost never. If you need a custom effect:

1. Check whether existing effects compose to it (`vibrancy + lens + colorControls` is more flexible than people assume).
2. Check whether `reflectiveGlass` or SDF covers it.
3. Only then write a custom `expect class` with AGSL + SkSL actuals — see `advanced.md`.

## Catalog apps

Platform-specific entrypoints in this repo:

| Platform | Run command |
|---|---|
| Android | `./gradlew :catalog:androidApp:installDebug` |
| Desktop | `./gradlew :catalog:desktopApp:run` |
| Web | `./gradlew :catalog:webApp:wasmJsBrowserRun` |
| iOS | open `catalog/iosApp/iosApp.xcodeproj` in Xcode |

Each one demonstrates the same shared component code. If a glass component looks correct on desktop and broken on Android, suspect the API-level fallback above before suspecting a bug.
