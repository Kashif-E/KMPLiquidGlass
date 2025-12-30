package com.kashif_e.backdrop.effects

import androidx.compose.ui.graphics.TileMode
import com.kashif_e.backdrop.BackdropEffectScope

/**
 * Apply a gaussian blur effect to the backdrop.
 * 
 * @param radius The blur radius in pixels. Higher values create more blur.
 * @param edgeTreatment How to handle pixels at the edge. Default is [TileMode.Clamp].
 */
expect fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode = TileMode.Clamp
)

/**
 * Apply opacity to the backdrop.
 * 
 * @param alpha The opacity value from 0.0 (fully transparent) to 1.0 (fully opaque).
 */
expect fun BackdropEffectScope.opacity(alpha: Float)

/**
 * Apply color adjustments to the backdrop.
 * 
 * @param brightness Brightness adjustment. 0 = no change, negative = darker, positive = brighter.
 * @param contrast Contrast adjustment. 1.0 = no change, <1 = less contrast, >1 = more contrast.
 * @param saturation Saturation adjustment. 1.0 = no change, 0 = grayscale, >1 = more saturated.
 */
expect fun BackdropEffectScope.colorControls(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
)

/**
 * Apply a vibrant color effect to the backdrop.
 * This increases saturation for a more vivid appearance.
 */
expect fun BackdropEffectScope.vibrancy()

/**
 * Apply a lens refraction effect to the backdrop, simulating light bending through glass.
 * 
 * @param refractionHeight The height of the refraction effect in pixels.
 * @param refractionAmount The amount of pixel displacement for the refraction.
 * @param depthEffect Whether to add depth-based shading.
 * @param chromaticAberration Whether to add chromatic aberration (color fringing).
 */
expect fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
)

/**
 * Apply exposure adjustment to the backdrop.
 * 
 * @param ev Exposure value adjustment. 0 = no change, positive = brighter, negative = darker.
 */
expect fun BackdropEffectScope.exposureAdjustment(ev: Float)

/**
 * Apply gamma adjustment to the backdrop.
 * 
 * @param power The gamma power value. 1.0 = no change.
 */
expect fun BackdropEffectScope.gammaAdjustment(power: Float)
