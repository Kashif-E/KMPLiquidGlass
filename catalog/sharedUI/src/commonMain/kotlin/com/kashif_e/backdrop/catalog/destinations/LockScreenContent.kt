package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.runtime.Composable

/**
 * Lock screen demo with SDF (Signed Distance Field) texture effect.
 * 
 * This demo showcases advanced glass effects using SDF shaders.
 * Note: The full SDF shader effect is only available on Android 13+ (API 33).
 * On other platforms, a simplified version is shown.
 */
@Composable
expect fun LockScreenContent(onBack: () -> Unit = {})
