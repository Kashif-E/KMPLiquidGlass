package com.kashif_e.backdrop.effects

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformCapabilities
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.min

/**
 * SkSL shader that applies lens refraction effect directly.
 * 
 * This shader samples the input content at displaced coordinates to create
 * a glass-like refraction effect. Unlike the DisplacementMap approach,
 * this works directly with the content shader.
 * 
 * Note: Since iOS/Metal doesn't support DisplacementMap filters reliably,
 * we use a RuntimeShader that computes the displacement and samples directly.
 */
private const val LensRefractionShaderString = """
uniform shader content;
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, float2(0.0))) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, float2(0.0)));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        // No displacement in the interior - sample at original position
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    // Calculate displacement intensity based on distance from edge
    float d = circleMap(1.0 - -sd / refractionHeight);
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
    
    // Compute displacement - negate to sample from inside (inward refraction)
    float2 displacement = -d * grad * refractionAmount;
    
    // Sample content at displaced coordinates
    return content.eval(coord + displacement);
}"""

/**
 * Applies a lens refraction effect to the backdrop.
 * 
 * This effect creates a glass-like refraction that distorts the background content
 * based on the shape's edges, simulating light bending through a curved surface.
 * 
 * On iOS, this uses a RuntimeShader that directly samples at displaced coordinates,
 * since DisplacementMap filters are not reliably supported on iOS/Metal.
 * 
 * @param refractionHeight The height/depth of the refraction zone from the edges
 * @param refractionAmount The intensity of the refraction distortion
 * @param depthEffect Whether to include a radial depth component to the refraction
 * @param chromaticAberration Whether to add chromatic dispersion (color separation)
 *        Note: Chromatic aberration is not yet supported on iOS
 */
actual fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean,
    chromaticAberration: Boolean
) {
    // Requires RuntimeShader support
    if (!PlatformCapabilities.supportsRuntimeShader) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding > 0f) {
        padding = maxOf(padding - refractionHeight, 0f)
    }

    val cornerRadii = cornerRadii
    if (cornerRadii != null) {
        // Create the lens refraction shader
        val effect = RuntimeEffect.makeForShader(LensRefractionShaderString)
        if (effect == null) return
        
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("size", size.width, size.height)
        builder.uniform("offset", -padding, -padding)
        builder.uniform("cornerRadii", cornerRadii[0], cornerRadii[1], cornerRadii[2], cornerRadii[3])
        builder.uniform("refractionHeight", refractionHeight)
        builder.uniform("refractionAmount", refractionAmount)
        builder.uniform("depthEffect", if (depthEffect) 1f else 0f)
        
        // Create RuntimeShader-based image filter that samples content at displaced coords
        val currentFilter = imageFilter
        val lensFilter = ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderNames = arrayOf("content"),
            inputs = arrayOf(currentFilter),
        )
        
        // Only apply if filter creation succeeded
        if (lensFilter != null) {
            imageFilter = lensFilter
        }
        // If null, silently skip the effect rather than crashing
    } else {
        throwUnsupportedSDFException()
    }
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
            min(topLeft, maxRadius),
            min(topRight, maxRadius),
            min(bottomRight, maxRadius),
            min(bottomLeft, maxRadius)
        )
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException("Only CornerBasedShape is supported in lens effects.")
}
