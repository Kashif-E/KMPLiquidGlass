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

## Magnifier — combined backdrops + scaled sampling

To refract through multiple sources at once — wallpaper + a paragraph of text + a draggable cursor — capture each into its own `LayerBackdrop` and combine. The magnification itself is done in `onDrawBackdrop` (scale the *sampled* backdrop), not in `layerBlock` (which would just scale the lens element).

This is the catalog's `MagnifierContent` verbatim:

```kotlin
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.shadow.InnerShadow
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun MagnifierDemo(backdrop: Backdrop /* = wallpaper */) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val backgroundColor = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF121212)

    val contentBackdrop = rememberLayerBackdrop()
    val cursorBackdrop = rememberLayerBackdrop()
    var offset by remember { mutableStateOf(Offset.Zero) }

    // The text panel — exposed as its own backdrop so the lens can sample it.
    BasicText(
        loremIpsumString,
        Modifier
            .layerBackdrop(contentBackdrop)
            .padding(24f.dp)
            .clip(RoundedCornerShape(32f.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .padding(24f.dp),
        style = TextStyle(contentColor, 16f.sp)
    )

    // The cursor (a small accent-colored bar). The user drags this around.
    Box(
        Modifier
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
            }
            .draggable2D(rememberDraggable2DState { delta -> offset += delta })
            .layerBackdrop(cursorBackdrop)
            .background(accentColor, RoundedCornerShape(100.dp))
            .size(4f.dp, 24f.dp)
    )

    // The magnifier — floats 80dp above the cursor, samples wallpaper + text + cursor.
    Box(
        Modifier
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y - 80f.dp.toPx()
            }
            .drawBackdrop(
                backdrop = rememberCombinedBackdrop(backdrop, contentBackdrop, cursorBackdrop),
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    lens(
                        8f.dp.toPx(),
                        24f.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                },
                innerShadow = { InnerShadow(radius = 16f.dp) },
                onDrawBackdrop = { drawBackdrop ->
                    withTransform(
                        {
                            scale(1.5f, 1.5f)                                       // zoom 1.5×
                            translate(top = with(this@drawBackdrop) { -80f.dp.toPx() }) // peer at the cursor's actual y
                        },
                        drawBackdrop
                    )
                }
            )
            .size(128f.dp, 96f.dp)
    )
}
```

Key insight: `onDrawBackdrop` lets you transform the sampled backdrop *before* effects are applied. `scale(1.5f, 1.5f)` magnifies; `translate(top = -80.dp.toPx())` peers down at where the cursor actually is (the magnifier is rendered 80dp above; we counter-translate the sampling so the lens shows the cursor location, magnified). `lens` then applies real refraction over that magnified-and-translated content.

**Common mistake**: passing `layerBlock = { scaleX = 1.5f; scaleY = 1.5f }` instead. That scales the *lens element itself* — the lens looks bigger, but doesn't actually magnify what's beneath. Always do magnification in `onDrawBackdrop`.

---

## Control Center — drag-to-reveal grid with overshoot animation

The catalog's `ControlCenterContent` is the iOS Control Center pattern: drag down from the top → glass tiles slide in with a translucent dim behind, push past the rest position → tiles overshoot and exaggerate the lens. Three layered animations:

1. **`enterProgressAnimation`** — the raw drag value, can go below 0 or above 1. Mapped through `ProgressConverter.Default` (asymmetric exponential) outside [0..1] to give resistive overshoot.
2. **`safeEnterProgressAnimation`** — same value clamped to [0..1]. Used for things that shouldn't overshoot: alpha, dim opacity, lens radius.
3. **`progress`** — derived from `enterProgressAnimation`, this is what the layout reads.

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastRoundToInt
import com.kashif_e.backdrop.BackdropEffectScope

// Soft asymmetric clamp — used to resist past-edge dragging.
fun interface ProgressConverter {
    fun convert(progress: Float): Float
    companion object {
        val Default: ProgressConverter =
            ProgressConverter { progress ->
                (1f - kotlin.math.exp(-kotlin.math.abs(progress))) * progress.sign
            }
    }
}

@Composable
fun ControlCenterDemo(
    wallpaper: Painter,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor = Color.Black.copy(0.05f)
    val dimColor = Color.Black.copy(0.4f)

    val itemSpacing = 16.dp
    val itemSize = 68.dp
    val itemTwoSpan = itemSize * 2 + itemSpacing
    val itemShape = RoundedCornerShape(100.dp)

    val animationScope = rememberCoroutineScope()
    val enterProgress = remember { Animatable(1f) }
    val safeEnter = remember { Animatable(1f) }
    val progress by remember {
        derivedStateOf {
            val p = enterProgress.value
            when {
                p < 0f -> ProgressConverter.Default.convert(p)         // resist below 0
                p <= 1f -> p                                            // linear in range
                else -> 1f + ProgressConverter.Default.convert(p - 1f)  // resist above 1
            }
        }
    }
    val maxDragHeight = 1000f

    val backdrop = rememberLayerBackdrop()

    val glassEffects: BackdropEffectScope.() -> Unit = {
        val p = safeEnter.value
        vibrancy()
        lens(24.dp.toPx() * p, 48.dp.toPx() * p, depthEffect = true)
    }
    val glassLayer: GraphicsLayerScope.() -> Unit = {
        val p = progress
        val s = safeEnter.value
        translationY = -48.dp.toPx() * (1f - p)
        alpha = EaseIn.transform(s)
        scaleX /= 1f + 0.1f * (p - 1f).fastCoerceAtLeast(0f)
        scaleY *= 1f + 0.1f * (p - 1f).fastCoerceAtLeast(0f)
    }
    val glassSurface: DrawScope.() -> Unit = { drawRect(containerColor) }
    val glassShape = { itemShape }

    val spacerLayoutModifier = Modifier.layout { m, c ->
        val placeable = m.measure(c)
        val height = itemSpacing.roundToPx() +
            (32.dp.toPx() * (progress - 1f).fastCoerceAtLeast(0f)).fastRoundToInt()
        layout(c.minWidth, height) { placeable.place(0, 0) }
    }

    val backdropModifier = Modifier
        .draggable(
            rememberDraggableState { delta ->
                val target = enterProgress.value + delta / maxDragHeight
                animationScope.launch {
                    launch { enterProgress.snapTo(target) }
                    launch { safeEnter.snapTo(target.fastCoerceIn(0f, 1f)) }
                }
            },
            Orientation.Vertical,
            onDragStopped = { velocity ->
                val target = when {
                    velocity < 0f -> 0f
                    velocity > 0f -> 1f
                    else -> if (enterProgress.value < 0.5f) 0f else 1f
                }
                animationScope.launch {
                    launch {
                        enterProgress.animateTo(
                            target,
                            if (target > 0.5f) spring(0.5f, 300f, 0.5f / maxDragHeight)
                            else spring(1f, 300f, 0.01f),
                            velocity / maxDragHeight
                        )
                    }
                    launch { safeEnter.animateTo(target, spring(1f, 300f, 0.01f)) }
                }
            }
        )
        .drawWithContent {
            drawContent()
            drawRect(dimColor.copy(dimColor.alpha * safeEnter.value))
        }
        .graphicsLayer {
            val r = 4.dp.toPx() * safeEnter.value
            if (r > 0f) renderEffect = BlurEffect(r, r)
        }

    Box(Modifier.fillMaxSize()) {
        // Wallpaper IS the drag/blur/dim source.
        Image(
            painter = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .then(backdropModifier)
                .fillMaxSize()
        )
        // ... grid of glass tiles, each using glassShape/glassEffects/glassLayer/glassSurface.
        // (Catalog draws ~9 tiles in a 2x2 + 1 + 2x2 pattern; layout is ordinary Rows + Columns.)
        Column(
            Modifier
                .padding(top = 80.dp)
                .systemBarsPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            effects = glassEffects,
                            highlight = { Highlight.Default },
                            shadow = null,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpan)
                )
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            effects = glassEffects,
                            highlight = { Highlight.Default },
                            shadow = null,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpan)
                )
            }
            Spacer(spacerLayoutModifier)
            // … additional rows omitted; pattern repeats with sizes itemTwoSpan / itemSize.
        }
    }
}
```

What's notable:

- **Wallpaper takes the drag gesture.** Not the grid — the grid only draws. Dragging the wallpaper is what makes the "pull down to reveal" gesture feel like the wallpaper is being pulled away.
- **Wallpaper is also blurred + dimmed.** As the user drags, the *captured* wallpaper layer gets a `BlurEffect` and a dim overlay applied to it. Since the glass tiles sample this blurred-and-dimmed wallpaper, they automatically pick up the effect with no extra config.
- **Glass effects scale with `safeEnter`, layout scales with `progress`.** Visual effects shouldn't overshoot — that produces ugly artifacts; layout overshoots so the user sees rubber-band resistance.

For the full nine-tile layout, see `catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/destinations/ControlCenterContent.kt`.

---

## Adaptive Luminance Glass

The glass tile's color treatment changes based on what's behind it: bright wallpaper → dark, contrasted glass; dark wallpaper → bright, softened glass. The catalog drives this via a manual luminance value tied to drag-Y, but the same effect logic works with a real luminance sampler.

```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sign

@Composable
fun AdaptiveLuminanceGlass(backdrop: Backdrop) {
    val isLightTheme = !isSystemInDarkTheme()

    val luminance = remember { Animatable(if (isLightTheme) 0.7f else 0.3f) }
    val contentColor = remember {
        Animatable(if (isLightTheme) Color.Black else Color.White)
    }

    val animationScope = rememberCoroutineScope()
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }

    Box(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = {
                    // Map luminance ([0..1]) to a non-linear contrast/brightness curve.
                    val l = (luminance.value * 2f - 1f).let { sign(it) * it * it }
                    colorControls(
                        brightness =
                            if (l > 0f) lerp(0.1f, 0.5f, l)
                            else lerp(0.1f, -0.2f, -l),
                        contrast =
                            if (l > 0f) lerp(1f, 0f, l)
                            else 1f,
                        saturation = 1.5f
                    )
                    blur(
                        if (l > 0f) lerp(8.dp.toPx(), 16.dp.toPx(), l)
                        else lerp(8.dp.toPx(), 2.dp.toPx(), -l)
                    )
                    lens(24.dp.toPx(), size.minDimension / 2f, depthEffect = true)
                },
                highlight = { Highlight.Plain },
                layerBlock = {
                    val o = offsetAnim.value
                    translationX = o.x
                    translationY = o.y
                    scaleX = zoomAnim.value
                    scaleY = zoomAnim.value
                    rotationZ = rotationAnim.value
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
            )
            .pointerInput(animationScope) {
                fun Offset.rotateBy(angle: Float): Offset {
                    val rad = angle * (PI / 180)
                    val c = cos(rad)
                    val s = sin(rad)
                    return Offset((x * c - y * s).toFloat(), (x * s + y * c).toFloat())
                }
                detectTransformGestures { _, pan, gZoom, gRot ->
                    val tZoom = zoomAnim.value * gZoom
                    val tRot = rotationAnim.value + gRot
                    val tOffset = offsetAnim.value + pan.rotateBy(tRot) * tZoom

                    // Luminance proxy: how far the tile has been dragged on Y.
                    val normalizedY = (tOffset.y / 500f).coerceIn(-1f, 1f)
                    val newLum = (0.5f + normalizedY * 0.3f).coerceIn(0.2f, 0.8f)

                    animationScope.launch {
                        offsetAnim.snapTo(tOffset)
                        zoomAnim.snapTo(tZoom)
                        rotationAnim.snapTo(tRot)
                        luminance.animateTo(newLum, tween(300))
                        contentColor.animateTo(
                            if (newLum > 0.5f) Color.Black else Color.White,
                            tween(300)
                        )
                    }
                }
            }
            .size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            "luminance:\n${(luminance.value * 100).toInt() / 100f}",
            style = TextStyle(Color.Unspecified, 16.sp, textAlign = TextAlign.Center),
            color = { contentColor.value }
        )
    }
}
```

The interesting math is the `(luminance.value * 2f - 1f).let { sign(it) * it * it }` — that's a centered `[-1..1]` value squared while preserving sign. Squaring near 0.5 (mid-luminance) gives near-zero contrast/brightness change, while values near 0 or 1 push hard. That's how iOS does it.

To make this actually adaptive (sample the real backdrop luminance), replace the manual luminance with a small offscreen render that averages pixels under the tile, then feed the result into the same animator. Out of scope for this recipe — see [Modifier.onGloballyPositioned] + a Pixel-readback technique.

---

## Lock Screen Widget — full SDF setup

The catalog's `LockScreenContent` is the canonical "premium 3D-textured glass" demo: a draggable rectangular widget with SDF-driven bevel refraction over a wallpaper.

```kotlin
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.rememberSdfShader
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun LockScreenWidgetDemo() {
    val backdrop = rememberLayerBackdrop()
    var offset by remember { mutableStateOf(Offset.Zero) }

    // The SDF texture. Catalog ships sdf.png; bring your own for custom shapes.
    val sdfBitmap = imageResource(Res.drawable.sdf)
    val sdfShader = rememberSdfShader(sdfBitmap)

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(Res.drawable.system_home_screen_light),
            contentDescription = null,
            modifier = Modifier.layerBackdrop(backdrop).fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier
                .background(Color.Black.copy(alpha = 0.3f))
                .fillMaxSize()
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .padding(horizontal = 48.dp)
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .draggable2D(rememberDraggable2DState { delta -> offset += delta })
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(50.dp) },
                            effects = {
                                colorControls(
                                    brightness = -0.1f,
                                    contrast = 0.75f,
                                    saturation = 1.5f
                                )
                                blur(2.dp.toPx())
                                with(sdfShader) { apply() }
                            },
                            onDrawBackdrop = { drawBackdrop ->
                                drawBackdrop()
                                drawRect(Color.White.copy(alpha = 0.25f))
                            }
                        )
                        .aspectRatio(sdfShader.width.toFloat() / sdfShader.height.toFloat())
                        .fillMaxWidth()
                )
            }
            Box(Modifier.weight(1f))
        }
    }
}
```

Key constraint: `aspectRatio(sdfShader.width / sdfShader.height)`. The SDF texture has fixed bevel geometry baked in; if you stretch it to a different aspect, the bevels look wrong. Always honor the shader's dimensions.

The same pattern works with a live camera as backdrop — see "Camera / video as backdrop" below.

---

## Glass Playground — interactive parameter exploration

A useful pattern: build a sheet of `LiquidSlider`s that drive the parameters of a single glass element so you can dial in your effect numerically. The catalog's `GlassPlaygroundContent` does exactly this. The takeaway is the `exportedBackdrop` parameter on `drawBackdrop`:

```kotlin
val sheetBackdrop = rememberLayerBackdrop()
Column(
    Modifier.drawBackdrop(
        backdrop = backdrop,                  // sheet samples the wallpaper
        shape = { RoundedCornerShape(32.dp) },
        effects = { /* … */ },
        highlight = { Highlight.Plain },
        exportedBackdrop = sheetBackdrop,     // also exposes the *sheet's own glass* as a backdrop
        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
    )
) {
    LiquidSlider(
        value = { blurRadius },
        onValueChange = { blurRadius = it },
        valueRange = 0f..32f,
        visibilityThreshold = 0.01f,
        backdrop = sheetBackdrop,             // sliders refract through the sheet itself
    )
    // ... more sliders ...
}
```

`exportedBackdrop` lets the sliders' thumbs sample the same glass surface they're embedded in — they pick up the sheet's white frost, not just the underlying wallpaper. Without it, the sliders would refract straight through to the original backdrop and look disconnected from the sheet.

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
