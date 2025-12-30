package com.kashif_e.backdrop.platform

import androidx.compose.ui.graphics.Paint
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/**
 * iOS/Desktop Skia-backed implementation of platform render effects.
 */
object PlatformCapabilities {
    val supportsRuntimeShader: Boolean = true
    val supportsBlur: Boolean = true
    val supportsColorFilter: Boolean = true
    val supportsRenderEffect: Boolean = true
    val supportsBlurMaskFilter: Boolean = true
}

/**
 * Platform abstraction for blur mask filters used in shadow/highlight effects.
 */
sealed class PlatformBlurMaskFilter {
    data class Skia(val maskFilter: MaskFilter) : PlatformBlurMaskFilter()
    data object None : PlatformBlurMaskFilter()

    companion object {
        fun create(radius: Float, style: BlurStyle = BlurStyle.NORMAL): PlatformBlurMaskFilter {
            if (radius <= 0f) return None
            val blurMode = when (style) {
                BlurStyle.NORMAL -> FilterBlurMode.NORMAL
                BlurStyle.SOLID -> FilterBlurMode.SOLID
                BlurStyle.OUTER -> FilterBlurMode.OUTER
                BlurStyle.INNER -> FilterBlurMode.INNER
            }
            // Skia uses sigma (standard deviation), Android uses radius
            // Convert: sigma = radius / 2
            return Skia(MaskFilter.makeBlur(blurMode, radius / 2f)!!)
        }
    }
}

enum class BlurStyle {
    NORMAL, SOLID, OUTER, INNER
}

/**
 * Extension to apply a platform blur mask filter to a Compose Paint.
 * Note: On Skia/iOS, we use the native Skia MaskFilter via the paint's internal skia paint.
 */
fun Paint.setPlatformMaskFilter(filter: PlatformBlurMaskFilter?) {
    // Access the underlying Skia paint
    val skiaPaint = this.asFrameworkPaint()
    skiaPaint.maskFilter = when (filter) {
        is PlatformBlurMaskFilter.Skia -> filter.maskFilter
        PlatformBlurMaskFilter.None, null -> null
    }
}

sealed class PlatformRenderEffect {
    /** Skia ImageFilter-backed effect */
    data class Skia(val imageFilter: ImageFilter) : PlatformRenderEffect()
    data object None : PlatformRenderEffect()

    companion object {
        fun blur(radius: Float, tileMode: FilterTileMode = FilterTileMode.DECAL): PlatformRenderEffect {
            if (radius <= 0f) return None
            val filter = ImageFilter.makeBlur(radius, radius, tileMode)
            return Skia(filter)
        }

        fun colorFilter(colorFilter: ColorFilter): PlatformRenderEffect {
            val filter = ImageFilter.makeColorFilter(colorFilter, null, crop = null)
            return Skia(filter)
        }

        fun colorMatrix(matrix: FloatArray): PlatformRenderEffect {
            val cm = ColorMatrix(*matrix)
            val cf = ColorFilter.makeMatrix(cm)
            return colorFilter(cf)
        }

        /**
         * Create a render effect from a runtime shader that samples from input content.
         * This is the equivalent of Android's RenderEffect.createRuntimeShaderEffect(shader, "content").
         * 
         * @param shaderBuilder The RuntimeShaderBuilder with uniforms already set
         * @param childShaderName The name of the child shader uniform in the SkSL (typically "content")
         * @param input Optional input ImageFilter to chain (usually null for direct content sampling)
         */
        fun runtimeShaderWithInput(
            shaderBuilder: RuntimeShaderBuilder,
            childShaderName: String = "content",
            input: ImageFilter? = null
        ): PlatformRenderEffect {
            val filter = ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = shaderBuilder,
                shaderName = childShaderName,
                input = input
            )
            return Skia(filter)
        }

        fun runtimeShader(shader: PlatformRuntimeShader): PlatformRenderEffect {
            val skiaShader = shader.build() ?: return None
            // Wrap shader in an image filter via shader effect
            val filter = ImageFilter.makeShader(skiaShader, dither = false, crop = null)
            return Skia(filter)
        }

        fun chain(outer: PlatformRenderEffect, inner: PlatformRenderEffect): PlatformRenderEffect {
            val outerFilter = outer.asSkiaImageFilter() ?: return inner
            val innerFilter = inner.asSkiaImageFilter() ?: return outer
            val composed = ImageFilter.makeCompose(outerFilter, innerFilter)
            return Skia(composed)
        }
    }
}

class PlatformRuntimeShader private constructor(
    private val effect: RuntimeEffect?,
    private val builder: RuntimeShaderBuilder?
) {
    companion object {
        /**
         * Compile an SkSL shader string. Return a wrapper that can have uniforms set.
         */
        fun compile(source: String): PlatformRuntimeShader {
            return try {
                val effect = RuntimeEffect.makeForShader(source)
                val builder = RuntimeShaderBuilder(effect)
                PlatformRuntimeShader(effect, builder)
            } catch (e: Exception) {
                // Compilation failed; return a no-op wrapper
                PlatformRuntimeShader(null, null)
            }
        }
        
        /**
         * Create a shader from SkSL source. Returns null if compilation fails.
         */
        fun create(source: String): PlatformRuntimeShader? {
            return try {
                val effect = RuntimeEffect.makeForShader(source)
                val builder = RuntimeShaderBuilder(effect)
                PlatformRuntimeShader(effect, builder)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun setFloatUniform(name: String, vararg values: Float) {
        if (builder == null) return
        when (values.size) {
            1 -> builder.uniform(name, values[0])
            else -> builder.uniform(name, values)
        }
    }
    
    fun setFloatUniform(name: String, v1: Float, v2: Float) {
        builder?.uniform(name, floatArrayOf(v1, v2))
    }
    
    fun setFloatUniform(name: String, v1: Float, v2: Float, v3: Float, v4: Float) {
        builder?.uniform(name, floatArrayOf(v1, v2, v3, v4))
    }

    fun setColorUniform(name: String, argb: Int) {
        if (builder == null) return
        // Convert ARGB int to float4 (RGBA order expected by Skia)
        val a = ((argb shr 24) and 0xFF) / 255f
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        builder.uniform(name, floatArrayOf(r, g, b, a))
    }

    internal fun build(): org.jetbrains.skia.Shader? {
        return builder?.makeShader()
    }
    
    /**
     * Get the underlying RuntimeShaderBuilder for use with ImageFilter.makeRuntimeShader.
     * This is needed for effects that sample from input content (like refraction).
     */
    internal fun getBuilder(): RuntimeShaderBuilder? = builder
    
    /**
     * Create a PlatformRenderEffect that properly samples from input content.
     * This is the equivalent of Android's RenderEffect.createRuntimeShaderEffect(shader, "content").
     * 
     * @param childShaderName The name of the uniform shader in the SkSL (default "content")
     * @param input Optional input ImageFilter to chain
     */
    fun asRenderEffectWithInput(
        childShaderName: String = "content",
        input: ImageFilter? = null
    ): PlatformRenderEffect {
        val shaderBuilder = builder ?: return PlatformRenderEffect.None
        return PlatformRenderEffect.runtimeShaderWithInput(shaderBuilder, childShaderName, input)
    }
}

fun PlatformRenderEffect.asSkiaImageFilter(): ImageFilter? =
    when (this) {
        is PlatformRenderEffect.Skia -> imageFilter
        PlatformRenderEffect.None -> null
    }

/** Stub for Android interop; not used on iOS/Desktop */
fun PlatformRenderEffect.asAndroid(): Any? = null
