package com.kashif_e.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.BackdropEffectScope
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
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
private const val SdfRefractionShaderString = """
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
    // Map to SDF texture coordinates
    float2 p = coord / size * sdfTexSize;
    if (p.x < 0.0 || p.y < 0.0 || p.x >= sdfTexSize.x || p.y >= sdfTexSize.y) {
        return half4(0.0);
    }
    
    // Sample SDF texture: R=distance, GB=normal, A=alpha
    half4 v = sdfTex.eval(p);
    float sd = v.r * 2.0 - 1.0;
    v.a = smoothstep(0.5, 1.0, v.a);
    
    if (v.a <= 0.0) {
        return half4(0.0);
    }
    if (v.a < 1.0) {
        sd = 0.0;
    }
    
    // Extract normal from GB channels
    float2 normal = normalize(v.gb * 2.0 - 1.0);
    
    // Calculate refraction intensity using circle mapping
    float intensity = circleMap(1.0 - min(1.0, -sd * 1.5));
    float2 refractedCoord = coord - intensity * refractionHeight * normal;

    // Sample content with refraction
    half4 color = content.eval(refractedCoord) * v.a;
    
    // Apply bevel lighting for 3D effect
    float2 lightDir = float2(cos(lightAngle * 3.14159265 / 180.0), sin(lightAngle * 3.14159265 / 180.0));
    
    // Highlight facing the light
    float bevelIntensity = clamp(dot(normal, lightDir), 0.0, 1.0);
    color.rgb *= 1.0 + 0.5 * intensity * bevelIntensity;
    
    // Rim highlight on opposite side
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
    val sdfBitmap: ImageBitmap
) {
    private val skiaImage: Image by lazy {
        Image.makeFromBitmap(sdfBitmap.asSkiaBitmap())
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
        val effect = RuntimeEffect.makeForShader(SdfRefractionShaderString)
        if (effect == null) {
            // Shader compilation failed - skip effect silently
            return
        }
        
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("size", size.width, size.height)
        builder.uniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
        builder.uniform("refractionHeight", refractionHeight)
        builder.uniform("lightAngle", lightAngle)
        
        // Pass the SDF texture as a child shader
        builder.child("sdfTex", sdfTextureShader)
        
        val currentFilter = imageFilter
        
        // Create the SDF image filter with both content and SDF texture shaders
        val sdfFilter = ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderNames = arrayOf("content"),
            inputs = arrayOf(currentFilter),
        )
        
        if (sdfFilter != null) {
            imageFilter = sdfFilter
        }
    }
}

/**
 * Remember an SdfShader instance for the given drawable resource.
 */
@Composable
actual fun rememberSdfShader(resource: DrawableResource): SdfShader {
    val imageBitmap = imageResource(resource)
    return remember(resource) {
        SdfShader(imageBitmap)
    }
}
