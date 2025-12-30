package com.kashif_e.backdrop

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Scope for defining backdrop visual effects.
 * 
 * This is a platform-agnostic interface that provides common properties
 * for applying effects like blur, color filters, and lens refraction.
 * 
 * Platform implementations:
 * - Android: Uses RenderEffect and RuntimeShader
 * - iOS: Uses Skia ImageFilter
 */
expect sealed interface BackdropEffectScope : Density {

    /**
     * The size of the backdrop area.
     */
    val size: Size

    /**
     * The layout direction (LTR or RTL).
     */
    val layoutDirection: LayoutDirection

    /**
     * The shape of the backdrop.
     */
    val shape: Shape

    /**
     * Padding applied around the backdrop for effects that extend beyond bounds.
     */
    var padding: Float
}
