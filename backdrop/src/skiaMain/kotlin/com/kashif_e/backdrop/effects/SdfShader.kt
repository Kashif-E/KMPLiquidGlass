package com.kashif_e.backdrop.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.BackdropEffectScope
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader

/**
 * SkSL SDF (Signed Distance Field) shader for glass texture effects.
 * 
 * This is the Skia/Desktop/iOS/Web equivalent of the Android AGSL SdfShader.
 * It samples from an SDF texture to create advanced glass refraction effects
 * with bevel lighting for realistic 3D appearance.
 */
private const val SDF_REFRACTION_SHADER_STRING = """
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
}"""

/**
 * SDF Shader wrapper for Skia-based platforms (Desktop, iOS, Web).
 * 
 * @param sdfBitmap The SDF texture as an ImageBitmap
 */
actual class SdfShader(
    internal val sdfImageBitmap: ImageBitmap
) {
    actual val width: Int get() = sdfImageBitmap.width
    actual val height: Int get() = sdfImageBitmap.height

    // Cache the RuntimeEffect to avoid recompilation each frame
    private val runtimeEffect: RuntimeEffect? by lazy {
        RuntimeEffect.makeForShader(SDF_REFRACTION_SHADER_STRING)
    }

    private val skiaImage: Image by lazy {
        Image.makeFromBitmap(sdfImageBitmap.asSkiaBitmap())
    }

    // Create the SDF texture shader
    private val sdfTextureShader: Shader by lazy {
        skiaImage.makeShader(
            tmx = FilterTileMode.CLAMP,
            tmy = FilterTileMode.CLAMP,
            sampling = SamplingMode.LINEAR
        )
    }

    /**
     * Apply the SDF shader effect to the backdrop.
     * 
     * @param refractionHeight The height/intensity of the refraction effect in pixels.
     * @param lightAngle The angle of the light source in degrees (0-360).
     */
    fun BackdropEffectScope.apply(
        refractionHeight: Float = 48f.dp.toPx(),
        lightAngle: Float = 45f
    ) {
        if (size.width.isNaN() || size.height.isNaN() || size.width <= 0f || size.height <= 0f) {
            return
        }
        
        val effect = runtimeEffect ?: return

        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("size", size.width, size.height)
        builder.uniform("sdfTexSize", sdfImageBitmap.width.toFloat(), sdfImageBitmap.height.toFloat())
        builder.uniform("refractionHeight", refractionHeight)
        builder.uniform("lightAngle", lightAngle)

        // Pass the SDF texture as a child shader
        builder.child("sdfTex", sdfTextureShader)

        val currentFilter = imageFilter

        val sdfFilter = ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderNames = arrayOf("content"),
            inputs = arrayOf(currentFilter)
        )

        if (sdfFilter != null) {
            imageFilter = sdfFilter
        }
    }
}

/**
 * Remember an SdfShader instance for the given ImageBitmap.
 */
@Composable
actual fun rememberSdfShader(imageBitmap: ImageBitmap): SdfShader {
    return remember(imageBitmap) {
        SdfShader(imageBitmap)
    }
}
