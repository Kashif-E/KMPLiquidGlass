package com.kashif_e.backdrop.effects

import androidx.compose.ui.graphics.Color
import com.kashif_e.backdrop.BackdropEffectScope
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

private const val PROGRESSIVE_BLUR_SKSL = """
uniform shader content;
uniform float2 size;
uniform half4 tint;
uniform float tintIntensity;
uniform float fadeStart;
uniform float fadeEnd;

half4 main(float2 coord) {
    float normalizedY = coord.y / size.y;
    float blurAlpha = smoothstep(fadeStart, fadeEnd, normalizedY);
    float tintAlpha = smoothstep(fadeStart, fadeEnd, normalizedY);
    half4 blurred = content.eval(coord) * blurAlpha;
    return mix(blurred, tint * tintAlpha, tintIntensity);
}
"""

private val progressiveBlurEffect: RuntimeEffect? by lazy {
    RuntimeEffect.makeForShader(PROGRESSIVE_BLUR_SKSL)
}

actual fun BackdropEffectScope.progressiveBlur(
    blurRadius: Float,
    tintColor: Color,
    tintIntensity: Float,
    fadeStart: Float,
    fadeEnd: Float
) {
    // First apply the blur
    blur(blurRadius)
    
    // Then apply the alpha mask shader
    val runtimeEffect = progressiveBlurEffect ?: return
    
    val builder = RuntimeShaderBuilder(runtimeEffect)
    builder.uniform("size", size.width, size.height)
    builder.uniform("tint", tintColor.red, tintColor.green, tintColor.blue, tintColor.alpha)
    builder.uniform("tintIntensity", tintIntensity)
    builder.uniform("fadeStart", fadeStart)
    builder.uniform("fadeEnd", fadeEnd)
    
    // Create the runtime shader effect
    val shader = builder.makeShader()
    
    // Use ImageFilter.makeRuntimeShader to apply the effect
    // Note: This requires the content as a child shader, which ImageFilter.makeRuntimeShader handles
    val filter = ImageFilter.makeRuntimeShader(
        runtimeShaderBuilder = builder,
        shaderNames = arrayOf("content"),
        inputs = arrayOf(null) // null means use the previous filter result
    )
    
    effect(filter)
}
