# Component recipes

Drop-in glass components, distilled from the catalog (`catalog/sharedUI/src/commonMain/kotlin/com/kashif_e/backdrop/catalog/`). Each recipe is the same code the published demo uses — same physics, same effect ordering, same edge-case handling.

**Before you start**, the interactive recipes (button, toggle, slider, bottom tabs) need three helper classes from `references/helpers.md`:
- `inspectDragGestures` — pointer pipeline.
- `InteractiveHighlight` — press / cursor offset tracker.
- `DampedDragAnimation` — value + velocity + scale springs for drag-based controls.

If you only want the visual look without the physics, the simpler `animateFloatAsState` versions work — replace `drag.pressProgress` with `pressProgress: Float by animateFloatAsState(if (pressed) 1f else 0f)` and skip `scaleX/scaleY/velocity`. But the catalog look depends on the helpers above; that's why they exist.

All recipes accept `backdrop: Backdrop` as a parameter. The caller owns the wallpaper / scrolling content / video stream — never have a component create its own backdrop unless the background is genuinely internal to it (e.g. a self-contained widget with a fixed image).

The imports listed in `SKILL.md` cover the library APIs used here. Helper imports (`InteractiveHighlight`, `DampedDragAnimation`) come from your project copy of those files.

---

## LiquidButton — pill button with drag-deformation

iOS's pill button isn't just "scale up on press" — it deforms based on where your finger drags. The `tanh` mapping clamps the deformation so it asymptotes near a fixed maximum no matter how far the finger goes. The translation is finger-position-driven; the scaleX/scaleY split tracks the *direction* of the drag so the button stretches toward the cursor.

```kotlin
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        // tanh: smooth saturating curve. translates with the finger up to ~maxOffset.
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        // Direction-aware stretch — the side the finger is on swells more.
                        val maxDragScale = 4f.dp.toPx() / size.height
                        val angle = atan2(offset.y, offset.x)
                        scaleX = scale +
                            maxDragScale * abs(cos(angle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)
                        scaleY = scale +
                            maxDragScale * abs(sin(angle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
                    }
                } else null,
                onDrawSurface = {
                    if (tint.isSpecified) {
                        // Hue-shift the backdrop, then a translucent fill — tints what's behind.
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
```

**Variants without changing the body:** circular icon button → wrap in `.clip(CircleShape)` and pass a fixed `.size(48.dp)` modifier from the call site. Solid-colored "primary" CTA → pass `surfaceColor = Color(0xFF0088FF)`. Hue-shifted glass tint (Apple-blue look) → pass `tint = Color(0xFF0088FF)`.

---

## LiquidToggle — pill switch with damped drag and lens-on-press

The thumb samples *two* backdrops: the wallpaper (so the glass refraction works) and the *track* (so the green-fill bleeds through the thumb). The track-backdrop is itself transformed inside `rememberBackdrop` so the on-press lens picks up a subtly compressed version of the track for that magnifier-on-grab effect.

```kotlin
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.InnerShadow
import com.kashif_e.backdrop.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceIn

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF34C759) else Color(0xFF30D158)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }

    val drag = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    // tap-only (no drag delta) → flip
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) didDrag = dragAmount.x != 0f
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(drag) {
        snapshotFlow { fraction }.collectLatest { drag.updateValue(it) }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }.collectLatest { isSelected ->
            val target = if (isSelected) 1f else 0f
            if (target != fraction) {
                fraction = target
                drag.animateToValue(target)
            }
        }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(modifier, contentAlignment = Alignment.CenterStart) {
        // Track — colored rail, exposed as its own backdrop so the thumb can sample it.
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(RoundedCornerShape(100.dp))
                .drawBehind {
                    val f = drag.value
                    drawRect(lerp(trackColor, accentColor, f))
                }
                .size(64f.dp, 28f.dp)
        )

        // Thumb — samples wallpaper + a press-scaled view of the track.
        Box(
            Modifier
                .graphicsLayer {
                    val f = drag.value
                    val pad = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(pad, pad + dragWidth, f)
                        else lerp(-pad, -(pad + dragWidth), f)
                }
                .semantics { role = Role.Switch }
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            // Squash the track when pressed, so the lens magnifies it.
                            val p = drag.pressProgress
                            val sx = lerp(2f / 3f, 0.75f, p)
                            val sy = lerp(0f, 0.75f, p)
                            scale(sx, sy) { drawBackdrop() }
                        }
                    ),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        val p = drag.pressProgress
                        blur(8f.dp.toPx() * (1f - p))
                        lens(
                            5f.dp.toPx() * p,
                            10f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val p = drag.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = p
                        )
                    },
                    shadow = {
                        Shadow(radius = 4f.dp, color = Color.Black.copy(alpha = 0.05f))
                    },
                    innerShadow = {
                        val p = drag.pressProgress
                        InnerShadow(radius = 4f.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val v = drag.velocity / 50f
                        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // Thumb is white at rest; fades to fully transparent under press
                        // so the magnified track shows through.
                        val p = drag.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - p))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
```

**Why two backdrops?** A single combined backdrop wouldn't let you scale the track independently from the wallpaper. The `rememberBackdrop(trackBackdrop) { ... scale() ... drawBackdrop() }` wrapper transforms only the track sampling — the wallpaper stays unscaled.

---

## LiquidSlider — track + thumb with overscroll squish

The slider thumb is essentially the toggle thumb with a horizontal track instead of a circular arc. Velocity-tracked springs give it the rubber-band overscroll feel: drag fast → scaleX stretches in the drag direction.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.fastRoundToInt

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        val drag = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) onValueChange(targetValue)
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) didDrag = dragAmount.x != 0f
                    val delta = (valueRange.endInclusive - valueRange.start) *
                        (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }
        LaunchedEffect(drag) {
            snapshotFlow { value() }.collectLatest { v ->
                if (drag.targetValue != v) drag.updateValue(v)
            }
        }

        // Track + filled portion. Tap on the track jumps to that position.
        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) *
                                (position.x / trackWidth)
                            val target =
                                (if (isLtr) valueRange.start + delta
                                 else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            drag.animateToValue(target)
                            onValueChange(target)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * drag.progress).fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
            )
        }

        // Thumb. Same drawBackdrop pattern as the toggle.
        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * drag.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) *
                            if (isLtr) 1f else -1f
                }
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val p = drag.pressProgress
                            val sx = lerp(2f / 3f, 1f, p)
                            val sy = lerp(0f, 1f, p)
                            scale(sx, sy) { drawBackdrop() }
                        }
                    ),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        val p = drag.pressProgress
                        blur(8f.dp.toPx() * (1f - p))
                        lens(
                            10f.dp.toPx() * p,
                            14f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val p = drag.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = p
                        )
                    },
                    shadow = { Shadow(radius = 4f.dp, color = Color.Black.copy(alpha = 0.05f)) },
                    innerShadow = {
                        val p = drag.pressProgress
                        InnerShadow(radius = 4f.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        // Velocity divisor of 10 (slider) vs 50 (toggle) — slider has more travel.
                        val v = drag.velocity / 10f
                        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val p = drag.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - p))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
```

Use it: `LiquidSlider({ blurRadius }, { blurRadius = it }, 0f..32f, 0.01f, backdrop)`.

---

## LiquidBottomTabs — drag-to-switch tab bar with spotlight

Three things make this look like Apple's bottom bar:

1. **Two glass rows.** One holds the actual tab buttons (icons + clicks). The other is invisible (`alpha(0f)`) — it's just there to be sampled as a backdrop so the *moving spotlight pill* can show a tinted color-filtered version of the active tab through itself. That's the "active tab glows blue inside the white indicator" trick.
2. **Drag-to-pan offset.** The whole bar can be dragged horizontally; on release it springs back. The drag offset is converted to a sublinear `EaseOut` curve so it resists past the edges.
3. **Z-order.** Background bar → invisible tinted row (the *backdrop source* for the spotlight) → spotlight pill → visible tab content row. Since `BoxWithConstraints` draws children in declaration order, the spotlight has to be drawn *before* the buttons, but the tinted-row trick means the active button still appears recolored under the spotlight.

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        // Drag-pan offset: when the user drags the whole bar, this animates back to 0 on release.
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }

        val drag = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt()
                        .fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }.collectLatest { currentIndex = it }
        }
        LaunchedEffect(drag) {
            snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
                drag.animateToValue(index.toFloat())
                onTabSelected(index)
            }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (drag.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (drag.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // Layer 1 — empty glass bar (the visible frosted pill).
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val p = drag.pressProgress
                        val s = lerp(1f, 1f + 16f.dp.toPx() / size.width, p)
                        scaleX = s
                        scaleY = s
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Layer 2 — invisible "tinted" row, exported as tabsBackdrop.
        // Used purely so the spotlight (layer 3) can sample a colored copy of the icons.
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, drag.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(100.dp) },
                        effects = {
                            val p = drag.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx() * p, 24f.dp.toPx() * p)
                        },
                        highlight = {
                            val p = drag.pressProgress
                            Highlight.Default.copy(alpha = p)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Layer 3 — the spotlight pill, sampling wallpaper + tabsBackdrop.
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) drag.value * tabWidth + panelOffset
                        else size.width - (drag.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        val p = drag.pressProgress
                        lens(
                            10f.dp.toPx() * p,
                            14f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val p = drag.pressProgress
                        Highlight.Default.copy(alpha = p)
                    },
                    shadow = { Shadow(alpha = drag.pressProgress) },
                    innerShadow = {
                        val p = drag.pressProgress
                        InnerShadow(radius = 8f.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val v = drag.velocity / 10f
                        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val p = drag.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - p
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * p))
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
```

Use it:

```kotlin
var selected by rememberSaveable { mutableIntStateOf(0) }
LiquidBottomTabs(
    selectedTabIndex = { selected },
    onTabSelected = { selected = it },
    backdrop = backdrop,
    tabsCount = 3
) {
    repeat(3) { i ->
        LiquidBottomTab({ selected = i }) {
            Box(Modifier.size(24.dp).paint(myIcon, colorFilter = ColorFilter.tint(Color.White)))
        }
    }
}
```

---

## Frosted dialog (heavy glass)

The dim layer goes on the **wallpaper's** modifier chain (after `layerBackdrop`), not on the dialog's parent. That way the dim is recorded into the captured backdrop layer; the visible wallpaper *and* the wallpaper-as-sampled-by-the-dialog are both dimmed by the same pass. iOS's exact behavior.

If you put the dim on the dialog's own parent `Box` with `drawWithContent { drawContent(); drawRect(dim) }`, you'll instead dim the dialog itself — `drawContent()` paints the dialog, then dim paints over it.

```kotlin
import com.kashif_e.backdrop.effects.colorControls
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FrostedDialog(
    backdrop: Backdrop,
    title: String,
    message: String,
    confirmText: String = "Okay",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = !isSystemInDarkTheme(),
) {
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)

    Column(
        modifier
            .padding(40f.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(48f.dp) },
                effects = {
                    colorControls(
                        brightness = if (isLightTheme) 0.2f else 0f,
                        saturation = 1.5f
                    )
                    blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                    lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                onDrawSurface = { drawRect(containerColor) }
            )
            .fillMaxWidth()
    ) {
        BasicText(
            title,
            Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
            style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
        )
        BasicText(
            message,
            Modifier
                .then(
                    // Dark theme: lighten the body text via Plus blend so it pops over the glass.
                    if (isLightTheme) Modifier
                    else Modifier.graphicsLayer(blendMode = BlendMode.Plus)
                )
                .padding(24f.dp, 12f.dp, 24f.dp, 12f.dp),
            style = TextStyle(contentColor.copy(0.68f), 15f.sp),
            maxLines = 5
        )
        Row(
            Modifier.padding(24f.dp, 12f.dp, 24f.dp, 24f.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel — secondary, translucent
            Row(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(containerColor.copy(0.2f))
                    .clickable(onClick = onCancel)
                    .height(48f.dp)
                    .weight(1f)
                    .padding(horizontal = 16f.dp),
                horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(cancelText, style = TextStyle(contentColor, 16f.sp))
            }
            // Confirm — primary, solid accent
            Row(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(accentColor)
                    .clickable(onClick = onConfirm)
                    .height(48f.dp)
                    .weight(1f)
                    .padding(horizontal = 16f.dp),
                horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(confirmText, style = TextStyle(Color.White, 16f.sp))
            }
        }
    }
}

// Demo: dim is drawn into the wallpaper's modifier chain, after layerBackdrop.
@Composable
fun FrostedDialogDemo(wallpaper: Painter) {
    val backdrop = rememberLayerBackdrop()
    val isLightTheme = !isSystemInDarkTheme()
    val dimColor =
        if (isLightTheme) Color(0xFF29293A).copy(0.23f)
        else Color(0xFF121212).copy(0.56f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)            // capture happens here
                .drawWithContent {
                    drawContent()                    // wallpaper drawn (and recorded)
                    drawRect(dimColor)               // dim drawn over wallpaper (also recorded)
                }
                .fillMaxSize()
        )
        FrostedDialog(
            backdrop = backdrop,
            title = "Dialog Title",
            message = "Confirm this action.",
            onConfirm = {},
            onCancel = {}
        )
    }
}
```

The `colorControls(brightness = 0.2f, saturation = 1.5f)` numbers aren't arbitrary — they brighten and saturate the sampled-and-dimmed wallpaper just enough that the dialog reads as a *bright frosted panel* over a darkened scene. If the dialog content looks too dark, raise `brightness`; if it looks washed-out, lower `saturation`.

---

## Glass card

A simpler container. Inline this for hero panels, info tiles, settings cards.

```kotlin
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32f.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = {
                    vibrancy()
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                }
            )
            .padding(20f.dp),
        content = content
    )
}
```

This matches the catalog's `ScrollContainerContent` / `LazyScrollContainerContent` cards exactly — they really are this minimal. No highlight, no shadow, no surface — pure refraction. Add `highlight = { Highlight.Default }` if the cards float on a busy backdrop and need definition.

---

## Glass search field

```kotlin
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    blur(6f.dp.toPx())
                    colorControls(saturation = 1.2f)
                },
                highlight = { Highlight.Default },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) }
            )
            .height(44f.dp)
            .padding(horizontal = 16f.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8f.dp)
    ) {
        Box(Modifier.size(16f.dp).background(Color.White.copy(0.6f), CircleShape))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) BasicText(
                    placeholder,
                    style = TextStyle(Color.White.copy(0.6f))
                )
                inner()
            },
            textStyle = TextStyle(Color.White, 16f.sp),
            singleLine = true,
        )
    }
}
```

---

## Sheet / drawer handle

```kotlin
@Composable
fun GlassDragHandle(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = { blur(2f.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            .size(36f.dp, 5f.dp)
    )
}
```

`drawPlainBackdrop` because the handle is decorative — no shadow, no highlight needed.

---

## Sticky frosted header over a `LazyColumn`

Glass over a constantly-invalidating backdrop (a scrolling list) is the worst case for `drawBackdrop`. The list re-records the captured layer every frame, so every effect runs every frame.

**Default to a lean stack:** `blur` only, optionally `colorControls` for tint. **Skip `vibrancy()`, `lens(...)`, and `chromaticAberration`** — they're per-frame shader passes that turn into jank on scroll. Use `Highlight.Plain` (flat stroke) over `Default` / `Ambient` (gradient) — saves a pass per frame.

```kotlin
@Composable
fun FrostedListHeader(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    height: Dp = 64f.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    colorControls(brightness = 0.05f, saturation = 1.4f)
                    blur(20f.dp.toPx())
                    // No vibrancy / no lens — list is moving.
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 8f.dp, color = Color.Black.copy(0.10f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.20f)) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicText(
            title,
            Modifier.padding(horizontal = 16f.dp),
            style = TextStyle(Color.Black, 17f.sp, FontWeight.SemiBold),
        )
    }
}

@Composable
fun FrostedListDemo() {
    val backdrop = rememberLayerBackdrop()
    val headerHeight = 64f.dp

    Box(Modifier.fillMaxSize()) {
        // List IS the backdrop — layerBackdrop captures the rendered LazyColumn,
        // so the header automatically refracts whatever rows are scrolled under it.
        LazyColumn(
            Modifier.fillMaxSize().layerBackdrop(backdrop),
            contentPadding = PaddingValues(top = headerHeight),
        ) {
            items(50) { i ->
                Row(
                    Modifier.fillMaxWidth().height(72f.dp).padding(16f.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Item $i", style = TextStyle(Color.Black, 16f.sp))
                }
            }
        }
        FrostedListHeader(
            title = "Inbox",
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            height = headerHeight,
        )
    }
}
```

---

## Scroll container with progressive blur edges

The catalog's `ProgressiveBlurContent` shows the simplest form — fade just the bottom half. Reverse `fadeStart`/`fadeEnd` to fade the top.

```kotlin
@Composable
fun GlassScrollContainer(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val tintColor = if (isLightTheme) Color.White else Color(0xFF808080)

    Box(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            content = { content() }
        )
        // Top fade — content blurs as it scrolls under the status bar.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        progressiveBlur(
                            blurRadius = 4f.dp.toPx(),
                            tintColor = tintColor,
                            tintIntensity = 0.8f,
                            fadeStart = 0f,    // top: full blur
                            fadeEnd = 1f       // bottom: clear
                        )
                    }
                )
                .height(96f.dp)
                .fillMaxWidth()
        )
    }
}
```

---

## Lazy scroll container (full-screen catalog grid)

The exact pattern the catalog uses on its `LazyScrollContainerContent` screen. No header — pure refracting cards.

```kotlin
@Composable
fun GlassLazyDemo(backdrop: Backdrop) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16f.dp),
        verticalArrangement = Arrangement.spacedBy(16f.dp)
    ) {
        item {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
        }
        items(100) {
            Box(
                Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(32f.dp) },
                        effects = {
                            vibrancy()
                            lens(16f.dp.toPx(), 32f.dp.toPx())
                        }
                    )
                    .height(160f.dp)
                    .fillMaxWidth()
            )
        }
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
```

Note: this *does* use `vibrancy()` + `lens(...)` over a scrolling list, which contradicts the "lean stack for scroll" guidance above. The catalog gets away with it because the *list itself* isn't the backdrop — the wallpaper is. The cards refract the static wallpaper, and the list scrolls the cards over it. So the backdrop layer doesn't re-record per frame; only the card transforms move. Confirm your scenario matches before using a heavy stack on a scrolling screen.

---

## Backdrop demo scaffold (catalog pattern)

The catalog's `BackdropDemoScaffold` is a useful pattern for any "show a glass UI over a wallpaper, with a fallback image picker" demo:

```kotlin
@Composable
fun BackdropDemoScaffold(
    modifier: Modifier = Modifier,
    initialPainter: Painter,
    onBack: (() -> Unit)? = null,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            initialPainter,
            contentDescription = null,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .then(modifier)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        content(backdrop)

        if (onBack != null) {
            LiquidButton(
                onBack,
                backdrop,
                Modifier.clip(CircleShape).padding(16f.dp).align(Alignment.TopStart),
                tint = Color(0xFF0088FF),
            ) {
                BasicText(
                    "Back",
                    Modifier.padding(horizontal = 16f.dp, vertical = 12f.dp),
                    style = TextStyle(Color.White, 16f.sp)
                )
            }
        }
    }
}
```

Use it: `BackdropDemoScaffold(initialPainter = painterResource(...)) { backdrop -> /* your glass UI */ }`. The pickable-image variant is in the full catalog source; the version here is the minimum scaffold.

---

## Layout choices: when to use which `Backdrop`

| Type | When |
|---|---|
| `rememberLayerBackdrop()` | 95% of cases — captures real composables (`Image`, `LazyColumn`, video). Pair with `Modifier.layerBackdrop(backdrop)` on the source. |
| `rememberCanvasBackdrop { onDraw }` | Procedural backdrops (gradients, generated patterns) — no composable to materialize. |
| `rememberCombinedBackdrop(a, b, c)` | Glass needs to refract several sources at once: wallpaper + content + cursor (magnifier), wallpaper + tinted-row (bottom tabs), wallpaper + colored track (toggle/slider). |
| `rememberBackdrop(inner) { drawBackdrop -> withTransform(...) { drawBackdrop() } }` | Wrap a backdrop with a custom transform. The toggle scales the track on press; the magnifier zooms in on what's beneath. |

---

## Common z-order traps (remember these)

1. **Bottom tabs**: empty bar → invisible tinted row (backdrop source) → spotlight pill → tab content row (drawn last, stays clickable + visible).
2. **Sticky list header**: `LazyColumn` with `layerBackdrop` *first*, header second so it draws on top.
3. **Modal dialog dim**: dim the wallpaper's `Image`, not the dialog's parent. Otherwise the dialog gets dimmed, not the surroundings.
4. **Magnifier**: scale the *backdrop sampling* (`onDrawBackdrop = { drawBackdrop -> withTransform({ scale(1.5f) }, drawBackdrop) }`), not the glass element (`layerBlock = { scaleX = 1.5f }`). The latter just makes the glass bigger; the former makes the lens magnify what's beneath.

For the magnifier, control center, lock screen, adaptive luminance, and glass playground recipes, see `references/advanced.md`.
