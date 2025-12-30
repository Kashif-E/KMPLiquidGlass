package com.kashif_e.backdrop.effects

import androidx.compose.ui.graphics.TileMode
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformCapabilities
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import org.jetbrains.skia.FilterTileMode

/**
 * Apply a blur effect to the backdrop.
 * 
 * @param radius The blur radius in pixels
 * @param edgeTreatment How to handle edges (clamp, repeat, mirror, decal)
 */
actual fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode
) {
    if (!PlatformCapabilities.supportsBlur) return
    if (radius <= 0f) return
    
    val skiaTileMode = edgeTreatment.toSkiaTileMode()

    if (skiaTileMode != FilterTileMode.CLAMP || imageFilter != null) {
        if (radius > padding) {
            padding = radius
        }
    }

    val current = imageFilter?.let { PlatformRenderEffect.Skia(it) } ?: PlatformRenderEffect.None
    val blur = PlatformRenderEffect.blur(radius, skiaTileMode)
    val chained = PlatformRenderEffect.chain(blur, current)
    imageFilter = chained.asSkiaImageFilter()
}

/**
 * Convert Compose TileMode to Skia FilterTileMode.
 */
private fun TileMode.toSkiaTileMode(): FilterTileMode = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
    TileMode.Decal -> FilterTileMode.DECAL
    else -> FilterTileMode.CLAMP
}

/**
 * Helper extension to get the Skia ImageFilter from a PlatformRenderEffect.
 */
private fun PlatformRenderEffect.asSkiaImageFilter() = when (this) {
    is PlatformRenderEffect.Skia -> imageFilter
    PlatformRenderEffect.None -> null
}
