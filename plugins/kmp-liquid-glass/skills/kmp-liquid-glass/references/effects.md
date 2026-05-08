# Effects reference

All functions are members of `BackdropEffectScope` and called inside `effects = { ... }` of `drawBackdrop` / `drawPlainBackdrop`. Order matters — effects compose top-to-bottom.

The scope inherits `Density`, so use `.dp.toPx()` for size-typed parameters (which all expect `Float` pixels).

---

## blur

```kotlin
fun blur(radius: Float, edgeTreatment: TileMode = TileMode.Clamp)
```

Gaussian blur over the sampled backdrop.

| Param | Range | Notes |
|---|---|---|
| `radius` | 0–32 dp typical | 0 = no blur. Above ~24 dp on a moving backdrop is GPU-heavy. |
| `edgeTreatment` | `Clamp`, `Decal`, `Mirror`, `Repeated` | Use `Clamp` (default) almost always. `Decal` lets pixels fade to transparent at edges — useful for a circular glass over a smaller backdrop region. |

**What each radius looks like:**

- 2 dp — softens fine detail; pairs with `lens` for a "wet glass" sheen
- 4–6 dp — typical for buttons / interactive pills
- 8–12 dp — typical for cards / nav pills
- 16+ dp — frosted dialog / sheet
- 24+ dp — heavy frost (control center, full-screen overlay)

**Gotcha:** very small radii (< 1 px) on Android API 31 may render unevenly. If you need "no blur," omit the call rather than passing 0.

---

## lens

```kotlin
fun lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
)
```

The "liquid" in liquid glass. Bends pixels at the shape's edge as if light were passing through curved glass. This is what makes the library look distinctively iOS rather than just a blurred rectangle.

| Param | Range | Notes |
|---|---|---|
| `refractionHeight` | 8–48 dp | How tall the bevel is. Larger = more curved-looking glass. |
| `refractionAmount` | 12–64 dp | How far pixels are displaced. Larger = more dramatic. |
| `depthEffect` | bool | Adds darkening on the underside for 3D feel. Use for dialogs/sheets, not for thin pills. |
| `chromaticAberration` | bool | RGB fringing at edges. Premium look — use sparingly: press states, magnifier, toggle thumbs. |

**Recipes:**

| Look | `(height, amount, depth, chroma)` |
|---|---|
| Tight pill button | `(12, 24, false, false)` |
| Thumb / chip | `(10, 14, false, true)` |
| Tab bar pill | `(24, 24, false, false)` |
| Frosted dialog | `(24, 48, true, false)` |
| Magnifier | `(8, 24, true, true)` |

**Requires Android API 33+** (uses AGSL). On API 31–32 the call no-ops; on iOS/Desktop/Web works everywhere.

---

## vibrancy

```kotlin
fun vibrancy()
```

Parameterless. Boosts saturation iOS-style — increases color in muted areas more than already-vivid ones. Add it *first* in the effect chain (before blur/lens) so the boosted color is what gets blurred and refracted.

Skip it on already-vivid backdrops (food photos, sunsets) — it can over-saturate.

---

## colorControls

```kotlin
fun colorControls(
    brightness: Float = 0f,    // -1.0 .. 1.0
    contrast: Float = 1f,      //  0.0 .. 2.0
    saturation: Float = 1f     //  0.0 .. 2.0
)
```

Photometric tone adjustments applied as a color matrix.

| Param | Default | Useful range |
|---|---|---|
| `brightness` | 0 | −0.2 to +0.3 for glass; outside that looks fake |
| `contrast` | 1 | 0.7 (soft) to 1.3 (punchy) |
| `saturation` | 1 | 0 = grayscale. 1.5 = iOS-dialog look. |

**Common pattern (frosted dialog):**

```kotlin
colorControls(brightness = 0.2f, saturation = 1.5f)   // light theme
colorControls(brightness = 0f, saturation = 1.5f)     // dark theme
```

Use either `colorControls` *or* `vibrancy()`, not usually both — they pull the same lever.

---

## opacity

```kotlin
fun opacity(alpha: Float)
```

`0f`–`1f`. Fades the *effect* (not the surface fill). Use to crossfade glass in/out without animating sizes.

---

## progressiveBlur

```kotlin
fun progressiveBlur(
    blurRadius: Float,
    tintColor: Color = Color.Transparent,
    tintIntensity: Float = 0f,
    fadeStart: Float = 1f,
    fadeEnd: Float = 0.5f
)
```

A blur whose *strength* fades vertically. Used for the soft top/bottom edges of scroll containers (so content blurs out instead of hard-clipping at the navigation bar / status bar).

| Param | Notes |
|---|---|
| `blurRadius` | Peak blur, in px. 4–8 dp typical. |
| `tintColor` | Color blended into the blurred region. `Color.White.copy(alpha = ...)` for a frosted top edge. |
| `tintIntensity` | 0–1 strength of the tint. |
| `fadeStart` | 0 = top of element, 1 = bottom. The blur is at full strength here. |
| `fadeEnd` | Where the blur fades to nothing. |

**Top-edge fade** (status bar bleed): `fadeStart = 0f, fadeEnd = 1f`.
**Bottom-edge fade** (above tab bar): `fadeStart = 1f, fadeEnd = 0f`.

Use with `drawPlainBackdrop` — there's nothing for highlights/shadows to wrap.

---

## exposureAdjustment / gammaAdjustment

```kotlin
fun exposureAdjustment(ev: Float)   // ±2 EV typical
fun gammaAdjustment(power: Float)   // 0.5 .. 2.0
```

Photographic tone shapers. Rarely needed for normal UI. Use when the backdrop is HDR-ish (a sunset, bright sky) and your glass needs to remain readable — `exposureAdjustment(-0.5f)` brings the sampled region into UI range.

---

## reflectiveGlass

```kotlin
fun reflectiveGlass(
    reflectionStrength: Float = 0.5f,    // 0..1
    distortionAmount: Float = 0.1f,
    chromaticAberration: Float = 0.02f,
    vignetteStrength: Float = 0.3f       // 0..1
)
```

A composite premium effect: subtle wave distortion + specular highlights + edge vignette. Designed for showcase / hero glass surfaces. Heavier than `lens` — don't use it on every chip in a list.

Defaults are intentionally tasteful; raise `distortionAmount` toward 0.3 for a wavy water look, `reflectionStrength` toward 0.8 for a polished mirror feel.

---

## SDF shader

The SDF effect is **not** a function on `BackdropEffectScope`. It's an instance you remember and `apply()` inside the effects scope:

```kotlin
val sdfBitmap = imageResource(Res.drawable.sdf_texture)
val sdfShader = rememberSdfShader(sdfBitmap)

Modifier.drawPlainBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(50.dp) },
    effects = {
        blur(2.dp.toPx())
        with(sdfShader) {
            apply(
                refractionHeight = 48.dp.toPx(),  // optional
                lightAngle = 45f                    // optional, degrees
            )
        }
    }
)
```

See `advanced.md` for SDF texture authoring and full parameter discussion.

---

## Composition cheatsheet

The order of effects matters because each operates on the previous result. A useful default mental model:

```
[backdrop pixels]
   ↓ vibrancy / colorControls   (fix colors first)
   ↓ blur                       (soften)
   ↓ lens                       (bend at edges)
   ↓ opacity                    (fade whole thing)
[output rendered behind shape]
```

Putting `lens` *before* `blur` produces sharp refracted edges with blurry centers — occasionally what you want, but unusual. Putting `vibrancy` *after* `blur` over-saturates the smear. The default ordering above is correct ~95% of the time.
