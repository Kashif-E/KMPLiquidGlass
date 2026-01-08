package com.kashif_e.backdrop.effects

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.BackdropEffectScope
import org.intellij.lang.annotations.Language

/**
 * Android SDF (Signed Distance Field) shader for creating advanced glass texture effects.
 * Requires Android 13+ (API 33) for RuntimeShader support.
 */
actual class SdfShader(
    internal val sdfBitmap: Bitmap
) {
    actual val width: Int get() = sdfBitmap.width
    actual val height: Int get() = sdfBitmap.height

    private val sdfTexture = BitmapShader(sdfBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    /**
     * Apply the SDF shader effect to the backdrop.
     * 
     * @param refractionHeight The height of the refraction effect in pixels.
     * @param lightAngle The angle of the light source in degrees.
     */
    fun BackdropEffectScope.apply(
        refractionHeight: Float = 48f.dp.toPx(),
        lightAngle: Float = 45f
    ) {
        if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = obtainRuntimeShader("SdfShader", SDF_SHADER_STRING).apply {
                setInputBuffer("sdfTex", sdfTexture)
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("lightAngle", lightAngle)
            }
            val currentEffect = renderEffect
            val sdfEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
            renderEffect = if (currentEffect != null) {
                RenderEffect.createChainEffect(sdfEffect, currentEffect)
            } else {
                sdfEffect
            }
        }
    }
}

/**
 * Remember an SdfShader instance for the given ImageBitmap.
 */
@Composable
actual fun rememberSdfShader(imageBitmap: ImageBitmap): SdfShader {
    return remember(imageBitmap) {
        val bitmap = Bitmap.createBitmap(imageBitmap.width, imageBitmap.height, Bitmap.Config.ARGB_8888)
        val buffer = IntArray(imageBitmap.width * imageBitmap.height)
        imageBitmap.readPixels(buffer)
        bitmap.setPixels(buffer, 0, imageBitmap.width, 0, 0, imageBitmap.width, imageBitmap.height)
        SdfShader(bitmap)
    }
}

@Language("AGSL")
private const val SDF_SHADER_STRING = """
uniform shader content;
uniform shader sdfTex;

uniform float2 size;
uniform float2 sdfTexSize;
uniform float refractionHeight;
uniform float lightAngle;

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    half2 p = coord / size * sdfTexSize;
    if (p.x < 0.0 || p.y < 0.0 || p.x >= sdfTexSize.x || p.y >= sdfTexSize.y) {
        return half4(0.0);
    }
    half4 v = sdfTex.eval(p);
    float sd = v.r * 2.0 - 1.0;
    v.a = smoothstep(0.5, 1.0, v.a);
    if (v.a <= 0.0) {
        return half4(0.0);
    }
    if (v.a < 1.0) {
        sd = 0.0;
    }
    float2 normal = normalize(v.gb * 2.0 - 1.0);
    
    float intensity = circleMap(1.0 - min(1.0, -sd * 1.5));
    float2 refractedCoord = coord - intensity * refractionHeight * normal;

    half4 color = content.eval(refractedCoord) * v.a;
    float2 lightDir = float2(cos(lightAngle * 3.1415926 / 180.0), sin(lightAngle * 3.1415926 / 180.0));
    float bevelIntensity = clamp(dot(normal, lightDir), 0.0, 1.0);
    color.rgb *= 1.0 + 0.5 * intensity * bevelIntensity;
    bevelIntensity = clamp(dot(normal, -lightDir), 0.0, 1.0);
    color.rgb *= 1.0 + 0.5 * bevelIntensity * min(1.0, smoothstep(1.0, 0.0, abs(intensity - 0.25) * 6.0));
    return color;
}
"""
