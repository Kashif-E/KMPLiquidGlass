package com.kashif_e.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformCapabilities
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import com.kashif_e.backdrop.platform.asAndroid

actual fun BackdropEffectScope.blur(
    @FloatRange(from = 0.0) radius: Float,
    edgeTreatment: TileMode
) {
    if (!PlatformCapabilities.supportsBlur) return
    if (radius <= 0f) return

    if (edgeTreatment != TileMode.Clamp || renderEffect != null) {
        if (radius > padding) {
            padding = radius
        }
    }

    val current = renderEffect?.let { PlatformRenderEffect.Android(it) } ?: PlatformRenderEffect.None
    val blur = PlatformRenderEffect.blur(radius, edgeTreatment.toAndroidTileMode())
    val chained = PlatformRenderEffect.chain(blur, current)
    renderEffect = chained.asAndroid()
}
