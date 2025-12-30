package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kashif_e.backdrop.catalog.BackdropDemoScaffold
import com.kashif_e.backdrop.catalog.Block
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

@Composable
fun AdaptiveLuminanceGlassContent(onBack: () -> Unit = {}) {
    val isLightTheme = !isSystemInDarkTheme()


    val luminanceAnimation = remember { Animatable(if (isLightTheme) 0.7f else 0.3f) }
    val contentColorAnimation = remember {
        Animatable(if (isLightTheme) Color.Black else Color.White)
    }

    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    BackdropDemoScaffold(onBack = onBack) { backdrop ->
        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
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
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = zoom
                        scaleY = zoom
                        rotationZ = rotation
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                )
                .pointerInput(animationScope) {
                    fun Offset.rotateBy(angle: Float): Offset {
                        val angleInRadians = angle * (PI / 180)
                        val cos = cos(angleInRadians)
                        val sin = sin(angleInRadians)
                        return Offset((x * cos - y * sin).toFloat(), (x * sin + y * cos).toFloat())
                    }

                    detectTransformGestures { _, pan, gestureZoom, gestureRotate ->
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value

                        val targetZoom = zoom * gestureZoom
                        val targetRotation = rotation + gestureRotate
                        val targetOffset = offset + pan.rotateBy(targetRotation) * targetZoom


                        val normalizedY = (targetOffset.y / 500f).coerceIn(-1f, 1f)
                        val newLuminance = (0.5f + normalizedY * 0.3f).coerceIn(0.2f, 0.8f)

                        animationScope.launch {
                            offsetAnimation.snapTo(targetOffset)
                            zoomAnimation.snapTo(targetZoom)
                            rotationAnimation.snapTo(targetRotation)
                            luminanceAnimation.animateTo(newLuminance, tween(300))
                            contentColorAnimation.animateTo(
                                if (newLuminance > 0.5f) Color.Black else Color.White,
                                tween(300)
                            )
                        }
                    }
                }
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Block {
                BasicText(
                    "luminance:\n${(luminanceAnimation.value * 100).toInt() / 100f}",
                    style = TextStyle(Color.Unspecified, 16.sp, textAlign = TextAlign.Center),
                    color = { contentColorAnimation.value }
                )
            }
        }
    }
}
