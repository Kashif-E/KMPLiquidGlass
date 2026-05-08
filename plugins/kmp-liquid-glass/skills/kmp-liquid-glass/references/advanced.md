# Advanced techniques

For when the standard `effects { blur + lens + vibrancy }` recipe doesn't go far enough.

## SDF shader (3D-textured glass)

The SDF (Signed Distance Field) shader produces realistic curved-glass refraction with bevel lighting. Use it for premium "physical object" looks: lock-screen widgets, hero modules, settings tiles, App Store cards.

### How it works

You provide a pre-computed SDF texture (an `ImageBitmap`) where each pixel encodes:

| Channel | Meaning |
|---|---|
| R | Distance to nearest shape edge (0.5 = on edge, < 0.5 inside, > 0.5 outside) |
| G | Surface-normal X component for refraction |
| B | Surface-normal Y component for refraction |
| A | Alpha mask |

The shader reads this texture per-pixel to bend the backdrop sample direction and add bevel highlights.

### Usage

```kotlin
val sdfBitmap = imageResource(Res.drawable.sdf_texture)
val sdfShader = rememberSdfShader(sdfBitmap)

Modifier
    .drawPlainBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(50.dp) },
        effects = {
            colorControls(brightness = -0.1f, contrast = 0.75f, saturation = 1.5f)
            blur(2.dp.toPx())
            with(sdfShader) {
                apply(
                    refractionHeight = 48.dp.toPx(),  // optional, default ~48 dp
                    lightAngle = 45f                    // optional, default 45°
                )
            }
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            drawRect(Color.White.copy(alpha = 0.25f))   // optional frost on top
        }
    )
    .aspectRatio(sdfShader.width.toFloat() / sdfShader.height.toFloat())
    .fillMaxWidth()
```

Use `drawPlainBackdrop` — `Highlight` and `Shadow` are redundant when the SDF is already producing 3D shading.

The element should match the SDF's aspect ratio (`sdfShader.width / sdfShader.height`); otherwise the texture stretches and the bevel looks wrong.

### Authoring an SDF texture

You generate SDF textures externally (Photoshop, custom shader, ImageMagick, distance-field libraries). The repo ships `sdf.png` for the lock-screen demo. Quick approach:

1. Author the silhouette as a hard-edged alpha shape.
2. Compute distance field (e.g. `convert mask.png -morphology Distance Euclidean output.png`).
3. Compute X/Y gradients of the distance field — these become G and B.
4. Pack into a single RGBA image.

For new shapes, simpler to start from the existing `sdf.png` in `catalog/sharedUI/composeResources/drawable/` and observe how the channels are organized.

### Platform notes

- **Android API 33+**: full SDF via AGSL `RuntimeShader` with texture sampling.
- **iOS / Desktop / Web**: full SDF via SkSL `RuntimeEffect`.
- **Android API < 33**: shader doesn't run; the glass falls back to whatever blur/color effects you also called.

---

## Combined backdrops (the magnifier pattern)

To refract through multiple sources at once — wallpaper + a paragraph of text + a draggable cursor — capture each into its own `LayerBackdrop` and combine:

```kotlin
val wallpaper = rememberLayerBackdrop()
val textLayer = rememberLayerBackdrop()
val cursorLayer = rememberLayerBackdrop()

// background
Image(..., Modifier.layerBackdrop(wallpaper))

// text panel
BasicText(content, Modifier.layerBackdrop(textLayer)...)

// movable cursor
Box(Modifier.layerBackdrop(cursorLayer)...)

// magnifier samples all three
Box(
    Modifier.drawBackdrop(
        backdrop = rememberCombinedBackdrop(wallpaper, textLayer, cursorLayer),
        shape = { RoundedCornerShape(100.dp) },
        effects = {
            lens(8.dp.toPx(), 24.dp.toPx(), depthEffect = true, chromaticAberration = true)
        },
        innerShadow = { InnerShadow(radius = 16.dp) },
        onDrawBackdrop = { drawBackdrop ->
            withTransform({
                scale(1.5f, 1.5f)             // zoom in
                translate(top = -80.dp.toPx())  // peer above
            }, drawBackdrop)
        }
    )
)
```

The key insight: `onDrawBackdrop` lets you transform the sampled backdrop before effects are applied. `scale(1.5f, 1.5f)` magnifies; `translate(...)` peers at a different region. The `lens` then applies real refraction *over* the magnified content.

---

## Wrapping a backdrop with a transform

`rememberBackdrop(inner) { drawBackdrop -> ... }` returns a new `Backdrop` that pre-transforms the inner one before sampling. Use this when *one* glass element needs a transformed view but the rest of the UI uses the unmodified backdrop.

```kotlin
val pressProgress = ... // 0..1

val animatedBackdrop = rememberBackdrop(trackBackdrop) { drawBackdrop ->
    val scaleX = lerp(2f / 3f, 0.75f, pressProgress)
    val scaleY = lerp(0f, 0.75f, pressProgress)
    scale(scaleX, scaleY) { drawBackdrop() }
}
```

Used in the toggle thumb: it samples a *vertically squashed* version of the colored track, which is what gives the squish-on-press feel.

---

## Custom shaders (expect/actual)

When a built-in effect doesn't exist, write your own. The library exposes platform `BackdropEffectScope` extension points:

```kotlin
// commonMain
expect class CustomShader {
    fun apply(scope: BackdropEffectScope)
}

// androidMain — AGSL (Android API 33+)
actual class CustomShader {
    private val shader = RuntimeShader("""
        uniform shader content;
        // ...your AGSL code...
        half4 main(float2 fragCoord) { return content.eval(fragCoord); }
    """.trimIndent())

    actual fun apply(scope: BackdropEffectScope) {
        scope.setRenderEffect(
            RenderEffect.createRuntimeShaderEffect(shader, "content")
        )
    }
}

// skiaMain — SkSL (iOS, Desktop, Web)
actual class CustomShader {
    private val effect = RuntimeEffect.makeForShader("""
        uniform shader content;
        half4 main(float2 fragCoord) { return content.eval(fragCoord); }
    """.trimIndent())

    actual fun apply(scope: BackdropEffectScope) {
        // ...build ImageFilter from effect...
    }
}
```

AGSL and SkSL are both GLSL-flavored. Most simple shaders port directly. For real examples, look at the library's `androidMain/.../effects/Lens.kt` (AGSL) and `skiaMain/.../Shaders.kt` (SkSL).

---

## Camera / video as backdrop

Anything you can put on screen can be a backdrop, including a camera preview. The catalog `CameraBackdropContent` does this:

```kotlin
val backdrop = rememberLayerBackdrop()

CameraPreview(
    modifier = Modifier.layerBackdrop(backdrop).fillMaxSize()
)

// Glass elements over the live camera feed
Box(Modifier.drawBackdrop(backdrop, ...))
```

Performance: camera frames change every ~16 ms, so the layer is re-captured each frame. Keep effects light (`blur` only, or `blur + small lens`). Avoid `colorControls` + `vibrancy` + heavy `lens` over live video on Android — it will drop frames on lower-end devices.

---

## Performance tuning

The library does the right thing by default, but glass over scrolling content is genuinely demanding work. Order of operations from cheapest to most expensive:

1. `drawPlainBackdrop` + `blur` only
2. `drawBackdrop` + `blur` + `Highlight.Plain`
3. `drawBackdrop` + `blur` + `lens` (no `chromaticAberration`)
4. `drawBackdrop` + `vibrancy` + `blur` + `lens(chromaticAberration = true)`
5. `drawBackdrop` + `colorControls` + `blur(>16dp)` + `lens(depthEffect = true)`
6. SDF shader + everything

For a list of 20 glass cards on Android API 33: tier 2–3 is fine. Tier 5+ over a fast-scrolling list will jank. Strategy:

- Keep heavy effects on focused/pressed/hero elements only.
- For lists, use a single backdrop and `drawPlainBackdrop` + `blur(8dp)` per row; reserve `lens` for the floating selection.
- Pre-bake `vibrancy()` into the source image where possible (raise saturation on the asset itself).
- Use `layerBlock` for animated transforms — never animate by recomposing modifier chains.

If you must animate effect parameters every frame, do it inside the `effects = { ... }` lambda by reading state — that's specifically how the lambdas are designed to be cheap.

---

## Multiple stacked glass surfaces

You can stack glass — a translucent dialog over a translucent tab bar over the wallpaper — but each layer needs its *own* backdrop and the deeper layers must sample the upper ones too:

```kotlin
val wallpaper = rememberLayerBackdrop()
val tabsLayer = rememberLayerBackdrop()

Image(..., Modifier.layerBackdrop(wallpaper))

// Tabs sample wallpaper, expose themselves to dialog
Row(
    Modifier
        .layerBackdrop(tabsLayer)        // makes this Row available as a backdrop
        .drawBackdrop(wallpaper, ...)    // and itself draws glass over wallpaper
)

// Dialog samples both wallpaper AND the glass tabs
Column(
    Modifier.drawBackdrop(
        rememberCombinedBackdrop(wallpaper, tabsLayer),
        shape = { RoundedCornerShape(48.dp) },
        effects = { /* ... */ }
    )
)
```

Be careful with cycles — a backdrop cannot sample itself. If A samples B and B samples A, neither will draw correctly.
