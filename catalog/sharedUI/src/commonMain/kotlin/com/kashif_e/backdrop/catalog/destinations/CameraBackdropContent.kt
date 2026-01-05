package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.ui.CameraPreview
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.reflectiveGlass
import com.kashif_e.backdrop.effects.rememberSdfShader
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.InnerShadow
import com.kashif_e.backdrop.shadow.Shadow
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.sdf
import org.jetbrains.compose.resources.imageResource

/**
 * Demo showing how to use a live camera preview as the backdrop for liquid glass effects.
 * This enables real-time frosted glass overlays on camera feeds with advanced shader effects
 * including reflective glass distortion, chromatic aberration, and vignette.
 * 
 * Uses CameraK's common API - works on Android, iOS, and Desktop.
 */
@Composable
fun CameraBackdropContent(onBack: () -> Unit) {
    val permissions: Permissions = providePermissions()
    val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var showReflectionDemo by remember { mutableStateOf(false) }
    
    val backdrop = rememberLayerBackdrop()
    
    // Request camera permission if not granted
    if (!cameraPermissionState.value) {
        permissions.RequestCameraPermission(
            onGranted = { cameraPermissionState.value = true },
            onDenied = { /* Handle denial */ }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.value) {
            // Camera preview as the backdrop using CameraK's common API
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                cameraConfiguration = {
                    setCameraLens(CameraLens.BACK)
                    setFlashMode(FlashMode.OFF)
                    setImageFormat(ImageFormat.JPEG)
                    setDirectory(Directory.PICTURES)
                },
                onCameraControllerReady = { controller ->
                    cameraController = controller
                }
            )
            
            // Glass overlay elements on top of camera
            var offset by remember { mutableStateOf(Offset.Zero) }
            val sdfBitmap = imageResource(Res.drawable.sdf)
            val sdfShader = rememberSdfShader(sdfBitmap)

            Column(
                Modifier
                    .background(Color.Black.copy(alpha = 0.3f))
                    .fillMaxSize()
            ) {
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
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
                                shape = { RoundedCornerShape(12.dp) },
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
                            // .aspectRatio(sdfShader.sdfBitmap.width.toFloat() / sdfShader.sdfBitmap.height.toFloat())
                            .fillMaxWidth()
                    )
                }
                Box(Modifier.weight(1f))
            }
        } else {
            // Permission not granted UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission required",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraGlassOverlay(
    backdrop: Backdrop,
    onBack: () -> Unit,
    onToggleLens: () -> Unit,
    showReflectionDemo: Boolean,
    onToggleReflection: () -> Unit
) {
    // Animated values for dynamic reflection effects
    val infiniteTransition = rememberInfiniteTransition(label = "reflection")
    val animatedReflection by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reflectionStrength"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar with back button - frosted glass with lens effect
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(16.dp.toPx())
                        lens(
                            refractionHeight = size.minDimension * 0.15f,
                            refractionAmount = size.minDimension * 0.3f,
                            depthEffect = true,
                            chromaticAberration = true
                        )
                        colorControls(brightness = 0.05f, saturation = 1.3f)
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow.Default }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            
            Text(
                text = "Camera Backdrop",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Text(
                text = "⟲",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onToggleLens() }
            )
        }
        
        // Center area with demo panels
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main glass panel - strong lens effect with chromatic aberration
                Box(
                    modifier = Modifier
                        .size(280.dp, 140.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(32.dp) },
                            effects = {
                                vibrancy()
                                blur(16.dp.toPx())
                                lens(
                                    refractionHeight = size.minDimension * 0.2f,
                                    refractionAmount = size.minDimension * 0.5f,
                                    depthEffect = true,
                                    chromaticAberration = true
                                )
                                colorControls(brightness = 0.1f, contrast = 1.1f, saturation = 1.4f)
                            },
                            highlight = { Highlight.Plain },
                            shadow = { Shadow.Default },
                            innerShadow = { InnerShadow.Default },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Liquid Glass",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Lens Refraction + Chromatic Aberration",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // NEW: Reflective glass panel with wave distortion effect
                if (showReflectionDemo) {
                    Box(
                        modifier = Modifier
                            .size(280.dp, 140.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(32.dp) },
                                effects = {
                                    // Apply the new reflective glass effect!
                                    reflectiveGlass(
                                        reflectionStrength = animatedReflection,
                                        distortionAmount = 0.15f,
                                        chromaticAberration = 0.025f,
                                        vignetteStrength = 0.4f
                                    )
                                    blur(8.dp.toPx())
                                    colorControls(brightness = 0.08f, saturation = 1.2f)
                                },
                                highlight = { Highlight.Ambient },
                                shadow = { Shadow.Default },
                                innerShadow = { InnerShadow.Default },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "✨ Wave Glass",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ripple + Chromatic + Vignette",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Toggle button for reflection demo
                Box(
                    modifier = Modifier
                        .clickable { onToggleReflection() }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(16.dp) },
                            effects = {
                                vibrancy()
                                blur(12.dp.toPx())
                                if (showReflectionDemo) {
                                    reflectiveGlass(
                                        reflectionStrength = 0.3f,
                                        distortionAmount = 0.08f,
                                        chromaticAberration = 0.015f,
                                        vignetteStrength = 0.2f
                                    )
                                }
                                colorControls(brightness = 0.1f, saturation = 1.3f)
                            },
                            highlight = { if (showReflectionDemo) Highlight.Ambient else Highlight.Default },
                            shadow = { Shadow.Default }
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showReflectionDemo) "Hide Reflection Demo" else "Show Reflection Demo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
        
        // Bottom controls - ambient highlight with lens refraction
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = {
                        vibrancy()
                        blur(12.dp.toPx())
                        lens(
                            refractionHeight = size.minDimension * 0.15f,
                            refractionAmount = size.minDimension * 0.35f,
                            depthEffect = true,
                            chromaticAberration = true
                        )
                        colorControls(brightness = 0.08f, saturation = 1.2f)
                    },
                    highlight = { Highlight.Ambient },
                    shadow = { Shadow.Default },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.05f)) }
                )
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glass buttons showcasing different effects
            GlassButton(backdrop, "Lens", EffectType.LENS)
            GlassButton(backdrop, "Wave", EffectType.REFLECTION)
            GlassButton(backdrop, "Blur", EffectType.BLUR)
            GlassButton(backdrop, "Glass", EffectType.COMBINED)
        }
    }
}

/**
 * Enum representing different glass effect types for demo buttons.
 */
private enum class EffectType {
    LENS,
    REFLECTION,
    BLUR,
    COMBINED
}

@Composable
private fun GlassButton(
    backdrop: Backdrop,
    label: String,
    effectType: EffectType
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    when (effectType) {
                        EffectType.LENS -> {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(
                                refractionHeight = size.minDimension * 0.25f,
                                refractionAmount = size.minDimension * 0.5f,
                                depthEffect = true,
                                chromaticAberration = true
                            )
                            colorControls(brightness = 0.15f, saturation = 1.3f)
                        }
                        EffectType.REFLECTION -> {
                            reflectiveGlass(
                                reflectionStrength = 0.5f,
                                distortionAmount = 0.12f,
                                chromaticAberration = 0.02f,
                                vignetteStrength = 0.3f
                            )
                            blur(6.dp.toPx())
                            colorControls(brightness = 0.1f, saturation = 1.2f)
                        }
                        EffectType.BLUR -> {
                            vibrancy()
                            blur(20.dp.toPx())
                            colorControls(brightness = 0.12f, saturation = 1.4f)
                        }
                        EffectType.COMBINED -> {
                            vibrancy()
                            blur(10.dp.toPx())
                            reflectiveGlass(
                                reflectionStrength = 0.25f,
                                distortionAmount = 0.08f,
                                chromaticAberration = 0.01f,
                                vignetteStrength = 0.2f
                            )
                            lens(
                                refractionHeight = size.minDimension * 0.15f,
                                refractionAmount = size.minDimension * 0.3f,
                                depthEffect = true,
                                chromaticAberration = false
                            )
                            colorControls(brightness = 0.15f, saturation = 1.3f)
                        }
                    }
                },
                highlight = { 
                    when (effectType) {
                        EffectType.LENS -> Highlight.Default
                        EffectType.REFLECTION -> Highlight.Ambient
                        EffectType.BLUR -> Highlight.Plain
                        EffectType.COMBINED -> Highlight.Default
                    }
                },
                shadow = { Shadow.Default },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
