package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.utils.ProgressConverter
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.ic_cyclone
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.kmp.shots.k.sensor.KSensor
import org.kmp.shots.k.sensor.SensorData.Accelerometer
import org.kmp.shots.k.sensor.SensorType
import org.kmp.shots.k.sensor.SensorUpdate
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun ControlCenterContent(onBack: () -> Unit = {}) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor = Color.Black.copy(0.05f)
    val dimColor = Color.Black.copy(0.4f)

    val itemSpacing = 16.dp
    val itemSize = 68.dp
    val itemTwoSpanSize = itemSize * 2 + itemSpacing
    val itemShape = RoundedCornerShape(100.dp)

    val innerItemSize = 56.dp
    val innerItemShape = RoundedCornerShape(100.dp)
    val innerItemIconScale = 0.8f

    val inactiveItemColor = Color.White.copy(0.2f)
    val activeItemColor = accentColor

    val airplaneModeIcon = painterResource(Res.drawable.ic_cyclone)
    val iconColorFilter = ColorFilter.tint(Color.White)

    val animationScope = rememberCoroutineScope()
    val enterProgressAnimation = remember { Animatable(1f) }
    val safeEnterProgressAnimation = remember { Animatable(1f) }
    val progress by remember {
        derivedStateOf {
            val progress = enterProgressAnimation.value
            when {
                progress < 0f -> ProgressConverter.Default.convert(progress)
                progress <= 1f -> progress
                else -> 1f + ProgressConverter.Default.convert(progress - 1f)
            }
        }
    }
    val maxDragHeight = 1000f

    // Gravity angle for highlight effect - updated by accelerometer sensor
    var gravityAngle by remember { mutableFloatStateOf(45f) }
    
    // Use KSensor for accelerometer data
    LaunchedEffect(Unit) {
        KSensor.registerSensors(
            types = listOf(SensorType.ACCELEROMETER),
            locationIntervalMillis = 16L // ~60fps
        )
            .catch { /* Sensor not available, use default angle */ }
            .collect { sensorUpdate ->
                when (sensorUpdate) {
                    is SensorUpdate.Data -> {
                        val accel = sensorUpdate.data as? Accelerometer ?: return@collect
                        // Smooth the angle using exponential moving average
                        val alpha = 0.15f
                        val newAngle = atan2(accel.y, accel.x) * (180f / PI.toFloat())
                        gravityAngle = gravityAngle * (1f - alpha) + newAngle * alpha
                    }
                    is SensorUpdate.Error -> { /* Sensor error, keep using current angle */ }
                }
            }
    }

    val glassShape = { itemShape }
    val glassHighlight = {
        Highlight(
            style = HighlightStyle.Default(
                angle = gravityAngle,
                falloff = 2f
            )
        )
    }
    val glassLayer: GraphicsLayerScope.() -> Unit = {
        val progress = progress
        val safeProgress = safeEnterProgressAnimation.value
        translationY = -48.dp.toPx() * (1f - progress)
        alpha = EaseIn.transform(safeProgress)
        scaleX /= 1f + 0.1f * (progress - 1f).fastCoerceAtLeast(0f)
        scaleY *= 1f + 0.1f * (progress - 1f).fastCoerceAtLeast(0f)
    }
    val glassSurface: DrawScope.() -> Unit = { drawRect(containerColor) }
    val glassEffects: BackdropEffectScope.() -> Unit = {
        val progress = safeEnterProgressAnimation.value
        vibrancy()
        lens(
            24.dp.toPx() * progress,
            48.dp.toPx() * progress,
            depthEffect = true
        )
    }

    val spacerLayoutModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val progress = progress
        val height =
            itemSpacing.roundToPx() +
                    (32.dp.toPx() * (progress - 1f).fastCoerceAtLeast(0f)).fastRoundToInt()
        layout(constraints.minWidth, height) {
            placeable.place(0, 0)
        }
    }
    val smallSpacerLayoutModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val progress = progress
        val height =
            itemSpacing.roundToPx() +
                    (16.dp.toPx() * (progress - 1f).fastCoerceAtLeast(0f)).fastRoundToInt()
        layout(constraints.minWidth, height) {
            placeable.place(0, 0)
        }
    }

    val backdropModifier = Modifier
        .draggable(
            rememberDraggableState { delta ->
                val targetProgress = enterProgressAnimation.value + delta / maxDragHeight
                animationScope.launch {
                    launch {
                        enterProgressAnimation.snapTo(targetProgress)
                    }
                    launch {
                        safeEnterProgressAnimation.snapTo(targetProgress.fastCoerceIn(0f, 1f))
                    }
                }
            },
            Orientation.Vertical,
            onDragStopped = { velocity ->
                val targetProgress = when {
                    velocity < 0f -> 0f
                    velocity > 0f -> 1f
                    else -> if (enterProgressAnimation.value < 0.5f) 0f else 1f
                }
                animationScope.launch {
                    launch {
                        enterProgressAnimation.animateTo(
                            targetProgress,
                            if (targetProgress > 0.5f) {
                                spring(0.5f, 300f, 0.5f / maxDragHeight)
                            } else {
                                spring(1f, 300f, 0.01f)
                            },
                            velocity / maxDragHeight
                        )
                    }
                    launch {
                        safeEnterProgressAnimation.animateTo(
                            targetProgress,
                            spring(1f, 300f, 0.01f)
                        )
                    }
                }
            }
        )
        .drawWithContent {
            val progress = safeEnterProgressAnimation.value

            drawContent()
            drawRect(dimColor.copy(dimColor.alpha * progress))
        }
        .graphicsLayer {
            val progress = safeEnterProgressAnimation.value

            val blurRadius = 4.dp.toPx() * progress
            if (blurRadius > 0f) {
                renderEffect = BlurEffect(blurRadius, blurRadius)
            }
        }

    BackdropDemoScaffold(
        backdropModifier,
        onBack = onBack
    ) { backdrop ->
        Column(
            Modifier
                .padding(top = 80.dp)
                .systemBarsPadding()
                .displayCutoutPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            effects = glassEffects,
                            highlight = glassHighlight,
                            shadow = null,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                        .padding(itemSpacing)
                ) {
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(inactiveItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.TopStart)
                    )
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(activeItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.TopEnd)
                    )
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(activeItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.BottomStart)
                    )
                }
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            effects = glassEffects,
                            highlight = glassHighlight,
                            shadow = null,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                )
            }

            Spacer(spacerLayoutModifier)

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    itemSpacing,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    effects = glassEffects,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    effects = glassEffects,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }

                    Spacer(smallSpacerLayoutModifier)

                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                effects = glassEffects,
                                highlight = glassHighlight,
                                shadow = null,
                                layerBlock = glassLayer
                            )
                            .size(itemTwoSpanSize, itemSize)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                effects = glassEffects,
                                highlight = glassHighlight,
                                shadow = null,
                                layerBlock = glassLayer,
                                onDrawSurface = glassSurface
                            )
                            .size(itemSize, itemTwoSpanSize)
                    )
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                effects = glassEffects,
                                highlight = glassHighlight,
                                shadow = null,
                                layerBlock = glassLayer,
                                onDrawSurface = glassSurface
                            )
                            .size(itemSize, itemTwoSpanSize)
                    )
                }
            }

            Spacer(spacerLayoutModifier)

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            effects = glassEffects,
                            highlight = glassHighlight,
                            shadow = null,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                )

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    effects = glassEffects,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    effects = glassEffects,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }

                    Spacer(smallSpacerLayoutModifier)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    effects = glassEffects,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }
                }
            }
        }
    }
}
