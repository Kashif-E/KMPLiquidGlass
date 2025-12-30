package com.kashif_e.backdrop.effects

import androidx.compose.ui.graphics.Color
import com.kashif_e.backdrop.BackdropEffectScope

/**
 * Apply a progressive blur effect that fades based on vertical position.
 * 
 * The blur fades from full opacity at the bottom to transparent at the top,
 * with an optional tint color overlay.
 * 
 * @param blurRadius The blur radius in pixels
 * @param tintColor Optional tint color to blend with the blur
 * @param tintIntensity Intensity of the tint (0.0 to 1.0)
 * @param fadeStart Where the fade starts (0.0 = top, 1.0 = bottom)
 * @param fadeEnd Where the fade ends (0.0 = top, 1.0 = bottom)
 */
expect fun BackdropEffectScope.progressiveBlur(
    blurRadius: Float,
    tintColor: Color = Color.Transparent,
    tintIntensity: Float = 0f,
    fadeStart: Float = 1f,
    fadeEnd: Float = 0.5f
)
