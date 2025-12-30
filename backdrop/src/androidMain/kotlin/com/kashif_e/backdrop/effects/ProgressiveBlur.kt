package com.kashif_e.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.RuntimeShaderCache
import com.kashif_e.backdrop.platform.PlatformCapabilities
import com.kashif_e.backdrop.platform.PlatformRenderEffect

private const val PROGRESSIVE_BLUR_SHADER = """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
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

actual fun BackdropEffectScope.progressiveBlur(
    blurRadius: Float,
    tintColor: Color,
    tintIntensity: Float,
    fadeStart: Float,
    fadeEnd: Float
) {
    // First apply the blur
    blur(blurRadius)
    
    // Then apply the alpha mask shader (Android 13+ only)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
        PlatformCapabilities.supportsRenderEffect &&
        this is RuntimeShaderCache
    ) {
        val shader = obtainRuntimeShader("ProgressiveBlur", PROGRESSIVE_BLUR_SHADER)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setColorUniform("tint", tintColor.toArgb())
        shader.setFloatUniform("tintIntensity", tintIntensity)
        shader.setFloatUniform("fadeStart", fadeStart)
        shader.setFloatUniform("fadeEnd", fadeEnd)
        
        val effect = RenderEffect.createRuntimeShaderEffect(shader, "content")
        applyPlatformEffect(PlatformRenderEffect.Android(effect))
    }
    // On older Android versions, just the blur is applied without the alpha mask
}
