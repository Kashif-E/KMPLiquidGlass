package com.kashif_e.backdrop.effects

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.annotation.FloatRange
import android.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.GammaAdjustmentShaderString
import com.kashif_e.backdrop.platform.PlatformCapabilities
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import kotlin.math.pow

fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    if (!PlatformCapabilities.supportsColorFilter) return
    val colorFilterEffect = PlatformRenderEffect.colorFilter(colorFilter)
    applyPlatformEffect(colorFilterEffect)
}

fun BackdropEffectScope.colorFilter(colorFilter: androidx.compose.ui.graphics.ColorFilter) {
    colorFilter(colorFilter.asAndroidColorFilter())
}

actual fun BackdropEffectScope.opacity(@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
    val colorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, alpha, 0f
        )
    )
    colorFilter(ColorMatrixColorFilter(colorMatrix))
}

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

actual fun BackdropEffectScope.vibrancy() {
    colorFilter(VibrantColorFilter)
}

private val VibrantColorFilter = colorControlsColorFilter(saturation = 1.5f)

actual fun BackdropEffectScope.exposureAdjustment(ev: Float) {
    val scale = 2f.pow(ev / 2.2f)
    val colorMatrix = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, 0f,
            0f, scale, 0f, 0f, 0f,
            0f, 0f, scale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    colorFilter(ColorMatrixColorFilter(colorMatrix))
}

actual fun BackdropEffectScope.gammaAdjustment(power: Float) {
    if (!PlatformCapabilities.supportsRuntimeShader) return

    val shader = obtainRuntimeShader("GammaAdjustment", GammaAdjustmentShaderString).apply {
        setFloatUniform("power", power)
    }
    effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
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
    val t = (0.5f - c * 0.5f + brightness) * 255f
    val s = saturation

    val cr = c * r
    val cg = c * g
    val cb = c * b
    val cs = c * s

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            cr + cs, cg, cb, 0f, t,
            cr, cg + cs, cb, 0f, t,
            cr, cg, cb + cs, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorMatrixColorFilter(colorMatrix)
}
