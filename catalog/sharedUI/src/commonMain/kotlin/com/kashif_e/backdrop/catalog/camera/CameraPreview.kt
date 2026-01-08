package com.kashif_e.backdrop.catalog.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live camera preview composable compatible with GraphicsLayer capture.
 */
@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: () -> Unit = {}
)

expect fun hasCameraPermission(): Boolean

@Composable
expect fun RequestCameraPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
)
