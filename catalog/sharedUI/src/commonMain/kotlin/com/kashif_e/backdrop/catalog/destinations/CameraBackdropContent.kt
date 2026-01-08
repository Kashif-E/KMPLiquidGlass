package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.catalog.camera.CameraPreview
import com.kashif_e.backdrop.catalog.camera.RequestCameraPermission
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.rememberSdfShader
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.sdf
import org.jetbrains.compose.resources.imageResource


@Composable
fun CameraBackdropContent(onBack: () -> Unit) {
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()

    // Request camera permission if not granted
    if (!cameraPermissionGranted) {
        RequestCameraPermission(
            onGranted = { cameraPermissionGranted = true },
            onDenied = { /* Handle denial */ }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            // Camera preview as the backdrop
            // Uses Image composable internally for proper GraphicsLayer capture
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                onCameraReady = {
                    // Camera is streaming
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
                            .aspectRatio(sdfShader.width.toFloat() / sdfShader.height.toFloat())
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



