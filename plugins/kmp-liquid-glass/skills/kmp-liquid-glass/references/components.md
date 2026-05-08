# Component recipes

Complete, working recipes for common glass components. Each accepts a `Backdrop` so the caller controls the background. Drop these in and adjust the *semantic* params (sizes, colors, content) — the effect parameters here are tuned to look right.

All examples assume the imports listed in `SKILL.md`.

---

## Glass card

A blurred, refracting container for arbitrary content. Used for hero panels, settings cards, info tiles.

```kotlin
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    tint: Color = Color.White.copy(alpha = 0.15f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(16.dp.toPx(), 24.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(0.15f)) },
                onDrawSurface = { drawRect(tint) }
            )
            .padding(20.dp),
        content = content
    )
}
```

Use a different `tint` for darker themes (`Color.Black.copy(alpha = 0.2f)` over light wallpapers).

---

## Glass pill button

iOS-style pill button with optional color tint. The `BlendMode.Hue` trick on the surface tints what's *behind* the glass, not the glass itself — that's the look you want.

```kotlin
@Composable
fun GlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val pressed = remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(if (pressed.value) 1f else 0f, label = "press")

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(
                        12.dp.toPx() + 4.dp.toPx() * pressProgress,
                        24.dp.toPx() + 4.dp.toPx() * pressProgress,
                        chromaticAberration = pressProgress > 0.01f
                    )
                },
                highlight = { Highlight.Ambient.copy(alpha = 0.6f + 0.4f * pressProgress) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(0.12f)) },
                layerBlock = {
                    val s = 1f + 0.04f * pressProgress
                    scaleX = s; scaleY = s
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = { onClick(); pressed.value = false }
            )
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
```

For a circular icon button: replace `RoundedCornerShape(100.dp)` with `CircleShape` and the height with a fixed `.size(48.dp)`.

---

## Frosted dialog (heavy glass)

Modal-style. The dim layer is the subtle but important detail: it must be drawn on the **wallpaper's** modifier chain (after `layerBackdrop`), so the dim is recorded into the captured backdrop. The glass dialog then samples a wallpaper that's already dimmed, and the visible wallpaper outside the dialog is dimmed by the same draw call. Result: a unified darkened mood with the dialog reading as glass over the dimmed scene — the iOS look.

If you put the dim on the dialog's own parent Box, you'll instead end up dimming the dialog itself. The dialog is just children of that Box, and `drawWithContent { drawContent(); drawRect(dim) }` will draw them first and then paint dim on top.

```kotlin
@Composable
fun FrostedDialog(
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = !isSystemInDarkTheme(),
    title: String,
    message: String,
    confirmText: String = "OK",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit,
) {
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
    val contentColor = if (isLightTheme) Color.Black else Color.White

    // The dialog itself — caller is responsible for the dim (see the demo below).
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier
                .padding(40.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(48.dp) },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .fillMaxWidth()
        ) {
            BasicText(title,
                Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp),
                style = TextStyle(contentColor, 24.sp, FontWeight.Medium))

            BasicText(message,
                Modifier.padding(24.dp, 12.dp, 24.dp, 12.dp),
                style = TextStyle(contentColor.copy(0.68f), 15.sp))

            Row(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cancel
                GlassButton(onDismissRequest, backdrop, Modifier.weight(1f)) {
                    BasicText(cancelText, style = TextStyle(contentColor, 16.sp))
                }
                // Confirm — solid accent for primary action
                Row(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF0088FF))
                        .clickable(onClick = onConfirm)
                        .height(48.dp).weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(confirmText, style = TextStyle(Color.White, 16.sp))
                }
            }
        }
    }
}

// Demo wiring — note the dim is on the *wallpaper's* modifier chain, after layerBackdrop.
@Composable
fun FrostedDialogDemo(
    wallpaper: Painter,
    isLightTheme: Boolean = !isSystemInDarkTheme(),
) {
    val backdrop = rememberLayerBackdrop()
    val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.23f) else Color(0xFF121212).copy(0.56f)

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .layerBackdrop(backdrop)            // capture happens here
                .drawWithContent {
                    drawContent()                   // wallpaper drawn (and recorded into the layer)
                    drawRect(dimColor)              // dim drawn over the wallpaper (and also recorded)
                }
                .fillMaxSize()
        )

        FrostedDialog(
            backdrop = backdrop,
            onDismissRequest = {},
            title = "Confirm",
            message = "Discard your changes?",
            onConfirm = {}
        )
    }
}
```

If you instead want the dialog's *content* (text + buttons) to read brighter than the surrounding dim, leave the dim where it is — the glass effect already lifts brightness via `colorControls(brightness = 0.2f, saturation = 1.5f)`, which is why those values are not 0/1.

---

## Glass toggle

A draggable pill switch. Uses two backdrops (the colored track + the wallpaper) combined for the thumb to sample.

```kotlin
@Composable
fun GlassToggle(
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val accent = Color(0xFF34C759)
    val track = Color(0xFF787878).copy(0.2f)
    val progress by animateFloatAsState(if (selected) 1f else 0f, label = "toggle")
    val pressed = remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(if (pressed.value) 1f else 0f, label = "press")

    val trackBackdrop = rememberLayerBackdrop()

    Box(modifier, contentAlignment = Alignment.CenterStart) {
        // Track — draws the colored rail and exposes itself as a backdrop
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(RoundedCornerShape(100.dp))
                .drawBehind { drawRect(lerp(track, accent, progress)) }
                .size(64.dp, 28.dp)
        )

        // Thumb — samples both wallpaper + track
        Box(
            Modifier
                .graphicsLayer { translationX = lerp(2.dp.toPx(), 38.dp.toPx(), progress) }
                .clickable { onSelect(!selected) }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        blur(8.dp.toPx() * (1f - pressProgress))
                        lens(
                            5.dp.toPx() * pressProgress,
                            10.dp.toPx() * pressProgress,
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Ambient.copy(alpha = pressProgress) },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(0.05f)) },
                    innerShadow = { InnerShadow(radius = 4.dp * pressProgress, alpha = pressProgress) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - pressProgress)) }
                )
                .size(40.dp, 24.dp)
        )
    }
}
```

For the full damped-drag version with proper gestures, see `catalog/sharedUI/src/commonMain/.../components/LiquidToggle.kt` in the repo.

---

## Bottom tab bar

**Z-order matters here.** A `Box` / `BoxWithConstraints` stacks children in declaration order — last child drawn is on top. If you put the indicator after the icons, it covers them. Three layers, in order:

1. **Empty bar** — just the frosted glass pill, no content
2. **Sliding indicator pill** — its own glass on top of the bar
3. **Tab content row** (icons + labels) — drawn LAST so it stays on top of both glass layers and remains tappable

```kotlin
@Composable
fun GlassBottomTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabs: List<TabItem>,                   // your icon + label pairs
    modifier: Modifier = Modifier,
) {
    val containerColor = Color(0xFFFAFAFA).copy(alpha = 0.4f)
    val horizontalPadding = 4.dp

    BoxWithConstraints(modifier.fillMaxWidth().height(64.dp)) {
        val tabsCount = tabs.size
        val tabWidthPx = with(LocalDensity.current) {
            (constraints.maxWidth.toFloat() - 2 * horizontalPadding.toPx()) / tabsCount
        }

        // 1. Empty bar — just the frosted glass pill.
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(0.15f)) },
                    onDrawSurface = { drawRect(containerColor) }
                )
        )

        // 2. Sliding indicator pill — own glass, drawn over the bar.
        val indicatorOffsetPx by animateFloatAsState(
            selectedIndex.toFloat() * tabWidthPx,
            label = "tab-indicator"
        )
        Box(
            Modifier
                .padding(horizontal = horizontalPadding, vertical = 6.dp)
                .graphicsLayer { translationX = indicatorOffsetPx }
                .fillMaxWidth(1f / tabsCount)
                .fillMaxHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        lens(12.dp.toPx(), 18.dp.toPx(), chromaticAberration = true)
                    },
                    highlight = { Highlight.Ambient },
                    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.35f) },
                    onDrawSurface = { drawRect(Color.White.copy(0.18f)) }
                )
        )

        // 3. Tab content row — DRAWN LAST so icons + labels sit on top of the indicator.
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val tint = if (selected) Color.White else Color.White.copy(0.7f)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onTabSelected(index) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Replace this Box with your real icon (Icon, Image, painter).
                    Box(Modifier.size(22.dp).background(tint, CircleShape))
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        tab.label,
                        style = TextStyle(
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                    )
                }
            }
        }
    }
}

data class TabItem(val label: String /*, val icon: ImageVector */)
```

Wrong z-order — what NOT to do:

```kotlin
// Don't do this. Indicator drawn AFTER tab content covers your icons and labels.
BoxWithConstraints {
    Row { /* tab buttons */ }
    Box { /* sliding indicator */ }   // ← drawn on top, hides the buttons under it
}
```

For the full draggable version with damped animation and a color-filtered "active tab through indicator" trick, see the catalog's `LiquidBottomTabs.kt`. The recipe above is the simpler "indicator behind icons" variant — it's what you want 90% of the time.

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
                    blur(6.dp.toPx())
                    colorControls(saturation = 1.2f)
                },
                highlight = { Highlight.Default },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) }
            )
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // search icon
        Box(Modifier.size(16.dp).background(Color.White.copy(0.6f), CircleShape))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) BasicText(placeholder, style = TextStyle(Color.White.copy(0.6f)))
                inner()
            },
            textStyle = TextStyle(Color.White, 16.sp),
            singleLine = true,
        )
    }
}
```

---

## FAB / circular icon button

```kotlin
@Composable
fun GlassFab(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Ambient },
                shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(0.2f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
            )
            .clickable(onClick = onClick, role = Role.Button)
            .size(56.dp),
        contentAlignment = Alignment.Center,
        content = icon
    )
}
```

---

## Sheet / drawer handle

Just a small pill — but it's the one tiny piece of glass everyone wants on a sheet.

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
                effects = {
                    blur(2.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            .size(36.dp, 5.dp)
    )
}
```

`drawPlainBackdrop` because the handle is decorative — no shadow / highlight needed.

---

## Sticky frosted header over a LazyColumn

Glass over a constantly-invalidating backdrop (a scrolling list) is the worst case for `drawBackdrop` — every scroll frame re-records the captured layer and re-runs every effect. Default to a **lean stack**: `blur` only, optionally `colorControls` for tint. Skip `vibrancy()`, `lens(...)`, and `chromaticAberration` for the scroll case. Use `Highlight.Plain` (flat stroke) over `Default` / `Ambient` (gradient) — saves a pass per frame.

```kotlin
@Composable
fun FrostedListHeader(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    // Order: tint first so it gets blurred (cheaper than tinting after).
                    colorControls(brightness = 0.05f, saturation = 1.4f)
                    blur(20.dp.toPx())
                    // No vibrancy / no lens / no chromaticAberration — list is moving.
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(0.10f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.20f)) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicText(
            title,
            Modifier.padding(horizontal = 16.dp),
            style = TextStyle(Color.Black, 17.sp, FontWeight.SemiBold),
        )
    }
}

@Composable
fun FrostedListDemo() {
    val backdrop = rememberLayerBackdrop()
    val headerHeight = 64.dp

    Box(Modifier.fillMaxSize()) {
        // 1. The list IS the backdrop. layerBackdrop captures the rendered LazyColumn
        //    — including any rows currently scrolled under the header position.
        LazyColumn(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(top = headerHeight),  // first row reachable
        ) {
            items(50) { i ->
                Row(
                    Modifier.fillMaxWidth().height(72.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Item $i", style = TextStyle(Color.Black, 16.sp))
                }
            }
        }

        // 2. Header pinned over the top of the list.
        FrostedListHeader(
            title = "Inbox",
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            height = headerHeight,
        )
    }
}
```

The trick is `Modifier.layerBackdrop(backdrop)` on the `LazyColumn` itself: that captures the rendered list as the backdrop, so the header at `Alignment.TopCenter` automatically samples whatever rows are currently scrolled under it. As the user scrolls, different rows pass through the captured layer and the header's blur smears them in real time. No separate state needed.

---

## Scroll container with progressive blur edges

```kotlin
@Composable
fun GlassScrollContainer(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            content = { content() }
        )

        // Top fade — content blurs as it scrolls under status bar
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        progressiveBlur(
                            blurRadius = 8.dp.toPx(),
                            tintColor = Color.White,
                            tintIntensity = 0.4f,
                            fadeStart = 0f,    // top: full blur
                            fadeEnd = 1f       // bottom: clear
                        )
                    }
                )
                .height(96.dp)
                .fillMaxWidth()
        )
    }
}
```

Reverse `fadeStart`/`fadeEnd` to put the fade above the bottom navigation instead.

---

## Layout choices: when to use a `LayerBackdrop` vs `CanvasBackdrop`

- **`rememberLayerBackdrop()`** — captures real composables (`Image`, `LazyColumn`, video). 95% of cases. Pair with `Modifier.layerBackdrop(backdrop)` on the source.
- **`rememberCanvasBackdrop { onDraw }`** — for fully procedural backdrops (gradients, generated patterns) where you don't want to materialize a composable just to be sampled.
- **`rememberCombinedBackdrop(a, b)`** — overlay two or more backdrops. Useful when a glass element needs to refract both the wallpaper and a moving cursor / floating sheet.
- **`rememberBackdrop(inner) { drawBackdrop -> ... }`** — wrap an existing backdrop with a custom transform. Used by the magnifier (zoom) and toggle (scale during press).
