package com.kashif_e.backdrop.effects

import android.graphics.RenderEffect
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.RoundedRectRefractionShaderString
import com.kashif_e.backdrop.RoundedRectRefractionWithDispersionShaderString
import com.kashif_e.backdrop.platform.PlatformCapabilities

/**
 * Applies a lens refraction effect to the backdrop.
 * 
 * This effect creates a glass-like refraction that distorts the background content
 * based on the shape's edges, simulating light bending through a curved surface.
 * 
 * @param refractionHeight The height/depth of the refraction zone from the edges
 * @param refractionAmount The intensity of the refraction distortion
 * @param depthEffect Whether to include a radial depth component to the refraction
 * @param chromaticAberration Whether to add chromatic dispersion (color separation)
 */
actual fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean,
    chromaticAberration: Boolean
) {
    // Requires RuntimeShader support (Android 13+ / API 33+)
    if (!PlatformCapabilities.supportsRuntimeShader) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader =
                if (!chromaticAberration) {
                    obtainRuntimeShader(
                        "Refraction",
                        RoundedRectRefractionShaderString
                    )
                } else {
                    obtainRuntimeShader(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    )
                }
            shader.apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("offset", -padding, -padding)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("refractionAmount", -refractionAmount)
                setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
                if (chromaticAberration) {
                    setFloatUniform("chromaticAberration", 1f)
                }
            }
            RenderEffect.createRuntimeShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

private val BackdropEffectScope.cornerRadii: FloatArray?
    get() {
        val shape = shape as? CornerBasedShape ?: return null
        val size = size
        val maxRadius = size.minDimension / 2f
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft =
            if (isLtr) shape.topStart.toPx(size, this)
            else shape.topEnd.toPx(size, this)
        val topRight =
            if (isLtr) shape.topEnd.toPx(size, this)
            else shape.topStart.toPx(size, this)
        val bottomRight =
            if (isLtr) shape.bottomEnd.toPx(size, this)
            else shape.bottomStart.toPx(size, this)
        val bottomLeft =
            if (isLtr) shape.bottomStart.toPx(size, this)
            else shape.bottomEnd.toPx(size, this)
        return floatArrayOf(
            topLeft.fastCoerceAtMost(maxRadius),
            topRight.fastCoerceAtMost(maxRadius),
            bottomRight.fastCoerceAtMost(maxRadius),
            bottomLeft.fastCoerceAtMost(maxRadius)
        )
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException("Only CornerBasedShape is supported in lens effects.")
}
