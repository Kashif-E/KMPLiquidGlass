package com.kashif_e.backdrop.effects

import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.GammaAdjustmentShaderString
import com.kashif_e.backdrop.platform.PlatformCapabilities
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import com.kashif_e.backdrop.platform.PlatformRuntimeShader
import com.kashif_e.backdrop.platform.asSkiaImageFilter
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.ImageFilter
import kotlin.math.pow

/**
 * Apply a color filter effect to the backdrop.
 */
fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    if (!PlatformCapabilities.supportsColorFilter) return
    val colorFilterEffect = PlatformRenderEffect.colorFilter(colorFilter)
    applyEffect(colorFilterEffect)
}

/**
 * Apply an opacity/alpha adjustment to the backdrop.
 */
actual fun BackdropEffectScope.opacity(alpha: Float) {
    // Skia ColorMatrix is 4x5 (20 floats): [R, G, B, A, translate] per row
    val matrix = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, alpha, 0f
    )
    val cm = ColorMatrix(*matrix)
    colorFilter(ColorFilter.makeMatrix(cm))
}

/**
 * Apply brightness, contrast, and saturation adjustments.
 */
actual fun BackdropEffectScope.colorControls(
    brightness: Float,
    contrast: Float,
    saturation: Float
) {
    if (brightness == 0f && contrast == 1f && saturation == 1f) {
        return
    }
    colorFilter(colorControlsColorFilter(brightness, contrast, saturation))
}

/**
 * Apply a vibrancy effect (increased saturation).
 */
actual fun BackdropEffectScope.vibrancy() {
    colorFilter(VibrantColorFilter)
}

private val VibrantColorFilter by lazy { colorControlsColorFilter(saturation = 1.5f) }

/**
 * Apply exposure adjustment (EV-based brightness scaling).
 */
actual fun BackdropEffectScope.exposureAdjustment(ev: Float) {
    val scale = 2f.pow(ev / 2.2f)
    // Skia ColorMatrix is 4x5 (20 floats): [R, G, B, A, translate] per row
    val matrix = floatArrayOf(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    val cm = ColorMatrix(*matrix)
    colorFilter(ColorFilter.makeMatrix(cm))
}

/**
 * Apply gamma adjustment.
 * Uses SkSL runtime shader to apply gamma correction to the backdrop.
 */
actual fun BackdropEffectScope.gammaAdjustment(power: Float) {
    if (!PlatformCapabilities.supportsRuntimeShader) return
    if (power == 1f) return  // No change needed
    
    val shader = PlatformRuntimeShader.create(GammaAdjustmentShaderString)
    if (shader != null) {
        shader.setFloatUniform("power", power)
        // Pass the current imageFilter as input so the shader samples from it
        val currentFilter = imageFilter
        val effect = shader.asRenderEffectWithInput(
            childShaderName = "content",
            input = currentFilter
        )
        // Set imageFilter directly since we're replacing the entire chain
        imageFilter = (effect as? PlatformRenderEffect.Skia)?.imageFilter
    }
}

private fun colorControlsColorFilter(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
): ColorFilter {
    val invSat = 1f - saturation
    val r = 0.213f * invSat
    val g = 0.715f * invSat
    val b = 0.072f * invSat

    val c = contrast
    // Skia ColorMatrix operates on normalized [0,1] values, NOT 0-255
    // The translation should be in the range -1 to 1
    val t = (0.5f - c * 0.5f + brightness)
    val s = saturation

    val cr = c * r
    val cg = c * g
    val cb = c * b
    val cs = c * s

    // Skia ColorMatrix is 4x5 (20 floats): [R, G, B, A, translate] per row
    val matrix = floatArrayOf(
        cr + cs, cg, cb, 0f, t,
        cr, cg + cs, cb, 0f, t,
        cr, cg, cb + cs, 0f, t,
        0f, 0f, 0f, 1f, 0f
    )
    val cm = ColorMatrix(*matrix)
    return ColorFilter.makeMatrix(cm)
}

/**
 * Internal helper to apply a platform effect to the current effect chain.
 */
internal fun BackdropEffectScope.applyEffect(effect: PlatformRenderEffect) {
    when (effect) {
        is PlatformRenderEffect.None -> return
        is PlatformRenderEffect.Skia -> {
            val current = imageFilter?.let { PlatformRenderEffect.Skia(it) }
            val merged = if (current != null) {
                PlatformRenderEffect.chain(effect, current)
            } else {
                effect
            }
            imageFilter = merged.asSkiaImageFilter()
        }
    }
}
