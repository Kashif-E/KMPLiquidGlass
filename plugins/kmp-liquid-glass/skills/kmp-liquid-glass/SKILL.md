---
name: kmp-liquid-glass
description: Build iOS-style liquid glass / frosted glass UI components in Compose Multiplatform using the io.github.kashif-mehmood-km:backdrop library. Use this skill whenever the user mentions liquid glass, frosted glass, Apple/iOS glass effect, glassmorphism, blur backdrop, lens refraction, vibrancy, the `backdrop` library, `drawBackdrop`, `rememberLayerBackdrop`, or asks for any glass-styled component (button, card, dialog, tab bar, toggle, slider, control center, lock screen widget, magnifier, navigation bar) in Compose, KMP, or Compose Multiplatform — even if they don't say "liquid glass" by name. Also use whenever the working directory is the KMPLiquidGlass repo or imports `com.kashif_e.backdrop.*`.
---

# KMP Liquid Glass — Building Glass Components

This skill turns any Compose Multiplatform agent into an effective user of the `backdrop` library: iOS-style liquid glass, frosted glass, vibrancy, lens refraction, progressive blur, SDF glass textures. Works on Android, iOS, Desktop, and Web with one common API.

## Mental model: two layers

Glass needs something to look through. The library makes you wire that up explicitly:

1. **Backdrop layer** — the content the glass *samples from* (an `Image`, gradient, video, scrolling list, etc.). Captured into a graphics layer via `Modifier.layerBackdrop(backdrop)`.
2. **Glass layer** — the element that draws the glass effect by sampling from the backdrop. Built with `Modifier.drawBackdrop(...)` or `Modifier.drawPlainBackdrop(...)`.

A glass element with no backdrop sampling source will show nothing useful. **Every glass component depends on a `LayerBackdrop` in scope.** Components should accept it as a parameter — never create their own internal backdrop unless they truly own the background (e.g. a self-contained widget with its own image).

## Setup

Gradle (commonMain):

```kotlin
commonMain.dependencies {
    implementation("io.github.kashif-mehmood-km:backdrop:0.0.1-alpha02")
}
```

Imports you'll use most:

```kotlin
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow
import com.kashif_e.backdrop.shadow.InnerShadow
```

## The minimum viable glass

```kotlin
@Composable
fun GlassDemo() {
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        // 1. Background captured into the backdrop
        Image(
            painter = painterResource(Res.drawable.wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize()
        )

        // 2. Glass element samples the backdrop
        Box(
            Modifier
                .align(Alignment.Center)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(12.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Ambient }
                )
                .size(200.dp, 120.dp)
        )
    }
}
```

That's it. Everything else is variation.

## How to design a glass component

When asked to build any glass component, follow this structure. **Always accept `backdrop: Backdrop` as a parameter** — the caller owns the background image / wallpaper / scrolling content; the component doesn't.

```kotlin
@Composable
fun MyGlassThing(
    /* ...semantic params... */,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { /* lambda returning Shape */ },
                effects = {
                    // Compose effects in the right order (see below)
                },
                highlight = { /* Optional Highlight or null */ },
                shadow = { /* Optional Shadow or null */ },
                innerShadow = { /* Optional InnerShadow or null */ },
                onDrawSurface = { /* Optional tint/overlay */ }
            )
            // sizing and content go after drawBackdrop
            .size(...)
    ) {
        // content composables
    }
}
```

### Effect ordering inside `effects = { ... }`

Effects compose in declaration order. The canonical iOS-style liquid stack is:

```kotlin
effects = {
    vibrancy()                                   // 1. boost saturation first
    blur(2.dp.toPx())                            // 2. soften
    lens(12.dp.toPx(), 24.dp.toPx())             // 3. bend/refract last
}
```

For a frostier dialog look, swap order and bump radii:

```kotlin
effects = {
    colorControls(brightness = 0.2f, saturation = 1.5f)
    blur(16.dp.toPx())
    lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
}
```

Rules of thumb:
- **Crisp/glossy glass** (buttons, toggles, tab bars): `blur(2–4dp)` + `lens(12–24dp)` + `vibrancy()`.
- **Frosted/heavy glass** (dialogs, sheets, control center): `blur(8–16dp)` + `lens(24–48dp)` ± `colorControls`.
- **Pure blur** (notification bar, scroll edge): `drawPlainBackdrop` + `blur` (or `progressiveBlur`).
- **Glass over scrolling content** (sticky list headers, search bars over feeds): `blur` + optional `colorControls` ONLY. **Skip `vibrancy`, `lens`, `chromaticAberration`** — they're per-frame shader passes that turn into jank on a constantly-invalidating backdrop. Prefer `Highlight.Plain` over the gradient highlights for the same reason.
- **Adding `chromaticAberration = true`** to `lens` gives a premium prismatic edge — great for press states, toggle thumbs, magnifiers. Avoid it on always-on or scrolling glass.

## Choosing the right entry point

| You want… | Use |
|---|---|
| Full glass with highlights/shadows | `Modifier.drawBackdrop(...)` |
| Plain effect, no decoration | `Modifier.drawPlainBackdrop(...)` |
| Just a blurred top/bottom edge of a scroll | `drawPlainBackdrop` + `progressiveBlur(...)` |
| 3D-textured glass (premium lock screen widget) | `drawPlainBackdrop` + `rememberSdfShader(...)` (see `references/advanced.md`) |
| Glass that reflects multiple layers (e.g. a scrolling list AND a fixed cursor) | `rememberCombinedBackdrop(layer1, layer2, ...)` |
| Glass with a transformed view of the backdrop (zoom/scale, like a magnifier) | `rememberBackdrop(inner) { drawBackdrop -> withTransform(...) { drawBackdrop() } }` |

## Effects reference (quick)

All effects are functions on `BackdropEffectScope` — call them inside `effects = { ... }`. Radii are `Float` pixels — use `.dp.toPx()`. See `references/effects.md` for parameter ranges and tradeoffs.

| Function | Purpose | Typical |
|---|---|---|
| `blur(radius, edgeTreatment)` | Gaussian blur | 2–16 dp |
| `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)` | Glass refraction at edges | height 12–24 dp, amount 24–48 dp |
| `vibrancy()` | iOS-style saturation bump | (parameterless) |
| `colorControls(brightness, contrast, saturation)` | Tone adjustments | brightness −1..1, contrast/sat 0..2 |
| `opacity(alpha)` | Fade the whole effect | 0..1 |
| `progressiveBlur(blurRadius, tintColor, tintIntensity, fadeStart, fadeEnd)` | Vertical-gradient blur for scroll edges | radius 4–8 dp |
| `exposureAdjustment(ev)` / `gammaAdjustment(power)` | Photographic tone | EV ±2, gamma 0.5–2 |
| `reflectiveGlass(reflectionStrength, distortionAmount, chromaticAberration, vignetteStrength)` | Curved reflective surface | defaults work |

## Decoration: highlights & shadows

`drawBackdrop` accepts three optional decorators. Each is a lambda returning `Highlight?`, `Shadow?`, `InnerShadow?` — return `null` to skip.

```kotlin
highlight = { Highlight.Ambient },                                     // soft glow stroke
shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(0.1f)) },    // drop shadow
innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.5f) },            // depth inside
```

Highlight presets:
- `Highlight.Default` — gradient-shaded stroke (default look on most glass)
- `Highlight.Ambient` — softer environmental glow (great for interactive elements)
- `Highlight.Plain` — simple solid stroke (use for dialogs, large frosted surfaces)

All three are `data class`es — `.copy(alpha = progress)` etc. is the idiomatic way to animate them.

## Surface fills and overlays

Pass `onDrawSurface = { ... }` to draw on top of the glass *after* the effects but *under* the children. This is how you tint glass:

```kotlin
onDrawSurface = {
    drawRect(Color.White.copy(alpha = 0.3f))   // light frost
}
```

For colored buttons (LiquidButton-style):

```kotlin
onDrawSurface = {
    if (tint.isSpecified) {
        drawRect(tint, blendMode = BlendMode.Hue)   // hue-shift the backdrop
        drawRect(tint.copy(alpha = 0.75f))
    }
}
```

`onDrawBehind`, `onDrawBackdrop`, and `onDrawFront` are also available for advanced layering — see `references/advanced.md`.

## Common component recipes

Each is a complete, working component. Inline simple ones; for the rest see `references/components.md`.

### A glass card

```kotlin
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(20.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(16.dp.toPx(), 24.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(0.15f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
            )
            .padding(20.dp),
        content = content
    )
}
```

### A glass pill button

```kotlin
@Composable
fun GlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },   // pill
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                highlight = { Highlight.Ambient },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                }
            )
            .clickable(onClick = onClick, role = Role.Button)
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
```

### A frosted dialog (heavy glass)

See `references/components.md` — uses `colorControls` + heavy blur + plain highlight + tinted surface fill, with optional dim layer behind.

### Bottom tab bar / navigation pill

See `references/components.md` for a working `LiquidBottomTabs`-style recipe (uses `rememberCombinedBackdrop` to layer the tab labels on top of the wallpaper, and animates `lens` on press).

### Progressive blur (scroll edges)

```kotlin
Box(
    Modifier
        .drawPlainBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                progressiveBlur(
                    blurRadius = 8.dp.toPx(),
                    tintColor = Color.White,
                    tintIntensity = 0.6f,
                    fadeStart = 0f,    // top fully blurred
                    fadeEnd = 1f       // bottom transparent
                )
            }
        )
        .height(96.dp)
        .fillMaxWidth()
)
```

## Animating a glass element

The library is built around lambda-based `effects`/`highlight`/`shadow` blocks specifically so they can read animation state and recompute per frame without re-allocating the modifier. The pattern from the catalog:

```kotlin
val pressProgress by animateFloatAsState(if (pressed) 1f else 0f)

Modifier.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(100.dp) },
    effects = {
        // read animated state inside the lambda — recomposes cheaply
        blur(8.dp.toPx() * (1f - pressProgress))
        lens(
            10.dp.toPx() * pressProgress,
            14.dp.toPx() * pressProgress,
            chromaticAberration = true
        )
    },
    highlight = {
        Highlight.Ambient.copy(alpha = pressProgress)
    },
    innerShadow = {
        InnerShadow(radius = 4.dp * pressProgress, alpha = pressProgress)
    },
    layerBlock = {
        scaleX = 1f + 0.05f * pressProgress
        scaleY = 1f + 0.05f * pressProgress
    }
)
```

Use `layerBlock` (a `GraphicsLayerScope.() -> Unit`) for transforms — they reuse the same underlying layer and don't trigger relayout.

## Platform support & API levels

| Platform | Status | Notes |
|---|---|---|
| Android API 31+ | blur, color controls work | Native `RenderEffect` |
| Android API 33+ | full library | Adds AGSL `RuntimeShader` for vibrancy, lens, SDF, custom shaders |
| iOS | full | Skia `ImageFilter` + `RuntimeEffect` |
| Desktop (JVM) | full | Skia |
| Web (wasm/JS) | full | Skia |

Below API 31 effects degrade silently — your component still draws, just without glass. If you need a manual fallback, gate on `Build.VERSION.SDK_INT` in your shared code via `expect`/`actual`.

See `references/platform.md` for full details.

## Common pitfalls — read this before debugging

1. **No backdrop set up.** If glass appears empty/black/transparent: did you put `Modifier.layerBackdrop(backdrop)` on the background content? The backdrop must be drawn somewhere in the same composition tree.

2. **Backdrop scope.** A `LayerBackdrop` only knows about content that flows through its `layerBackdrop` modifier. To layer multiple sources (e.g. wallpaper + scrolling content + a moving cursor), create separate `LayerBackdrop` instances and combine with `rememberCombinedBackdrop(a, b, c)`.

3. **`shape` is a lambda, not a `Shape`.** `shape = { RoundedCornerShape(24.dp) }` — easy to drop the braces by accident.

4. **`effects` is a receiver scope.** Call effects (`blur(...)`, `lens(...)`) inside the braces; they return `Unit`. You can't `effects = blur(...)`.

5. **Radii are `Float` pixels.** Use `.dp.toPx()` (the scope inherits `Density`). Passing raw `Dp` won't compile.

6. **Sizing comes after `drawBackdrop`.** The drawing modifier needs to know its bounds; chain `.size(...)` / `.fillMaxWidth()` after `drawBackdrop`, before any `.padding()` for content.

7. **Heavy stacks are expensive.** `vibrancy()` + `blur(16dp)` + `lens(48dp, depthEffect = true)` over a moving backdrop on Android API 33 is a real GPU workload. For lists/scrollers, use `drawPlainBackdrop` + just `blur` where possible, and only escalate effects on press/focus.

8. **Don't recreate the backdrop every recomposition.** `rememberLayerBackdrop()` returns a remembered instance — keep it at the screen-scaffold level and pass it down. Re-creating it discards the captured layer.

9. **Animation goes inside the effect lambdas, not in the modifier chain.** Reading `pressProgress` inside `effects = { ... }` works perfectly; pulling effects out into a `derivedStateOf<Modifier>` allocates per frame.

10. **Z-order in stacked glass: tappable / readable content goes LAST.** When stacking glass — e.g. a tab bar with a sliding indicator pill, or a panel with a moving cursor — `Box`/`BoxWithConstraints` draws children in declaration order, and the last child is on top. If you draw a glass indicator *after* your tab icons, the indicator will cover the icons. Layer order: (1) empty glass background, (2) glass indicator/decoration, (3) icons + labels + clickable surfaces. See the bottom tab recipe in `references/components.md`.

11. **Dim layers belong on the wallpaper, not on the dialog.** For modal dialogs that should appear over a darkened scene, draw the dim into the wallpaper Image's modifier chain — *after* `layerBackdrop` so the dim is recorded into the captured backdrop. If you put the dim on the dialog's parent `Box` with `drawWithContent { drawContent(); drawRect(dim) }`, you'll dim the dialog itself instead of the surroundings, because the dialog is what `drawContent()` rendered.

12. **Magnification is `onDrawBackdrop`, NOT `layerBlock`.** A magnifier needs to scale the *sampled backdrop* (so the user sees content beneath the lens enlarged), not the glass element itself. Use `onDrawBackdrop = { drawBackdrop -> withTransform({ translate(-dragOffset); scale(1.5f) }, drawBackdrop) }`. If you instead pass `layerBlock = { scaleX = 1.5f; scaleY = 1.5f }`, you'll scale the entire glass element — the lens looks bigger but doesn't magnify what's underneath. See `references/advanced.md`.

## When to dive into references

- `references/effects.md` — full parameter ranges, gotchas per effect, what each one looks like
- `references/helpers.md` — `inspectDragGestures`, `InteractiveHighlight`, `DampedDragAnimation` (catalog helper classes that drive the physics in the interactive recipes; copy them into your project)
- `references/components.md` — sample-faithful recipes: `LiquidButton` (tanh drag-deformation), `LiquidToggle` (track + scaled-track-backdrop), `LiquidSlider` (velocity-squish thumb), `LiquidBottomTabs` (double-row tinted-spotlight trick), frosted dialog, glass card, search field, sheet handle, sticky frosted list header, scroll container with progressive blur edges, lazy column grid, demo scaffold
- `references/advanced.md` — SDF shaders, magnifier (combined backdrops + `onDrawBackdrop` zoom), Control Center (drag-to-reveal with overshoot), Adaptive Luminance Glass, Lock Screen widget, Glass Playground (`exportedBackdrop` for sliders sampling their own sheet), camera/video as backdrop, custom shaders, performance tuning
- `references/platform.md` — exact API-level behavior, what degrades where, custom shader templates for AGSL/SkSL

When you've read SKILL.md and you're building anything beyond the inline recipes, open the matching reference. Don't guess — the parameters in this library have specific ranges and surprising defaults (e.g. `Highlight.width = 0.5dp` — small numbers).

For interactive recipes (button, toggle, slider, bottom tabs), the catalog look depends on the helpers in `references/helpers.md`. If you skip them and use plain `animateFloatAsState`, you get the visual glass but not the spring/squish/velocity feel — open `helpers.md` first.
