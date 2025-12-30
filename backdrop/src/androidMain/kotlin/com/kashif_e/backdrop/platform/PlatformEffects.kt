package com.kashif_e.backdrop.platform

import android.graphics.BlurMaskFilter
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.MaskFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Paint

/**
 * Platform abstraction shim to pave the path to Compose Multiplatform/iOS.
 * Currently implemented in terms of Android classes so existing behavior is preserved.
 */
object PlatformCapabilities {
    val supportsRuntimeShader: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val supportsBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsColorFilter: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsRenderEffect: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsBlurMaskFilter: Boolean = true // Always available on Android
}

/**
 * Platform abstraction for blur mask filters used in shadow/highlight effects.
 */
sealed class PlatformBlurMaskFilter {
    data class Android(val maskFilter: MaskFilter) : PlatformBlurMaskFilter()
    data object None : PlatformBlurMaskFilter()

    companion object {
        fun create(radius: Float, style: BlurStyle = BlurStyle.NORMAL): PlatformBlurMaskFilter {
            if (radius <= 0f) return None
            val blurType = when (style) {
                BlurStyle.NORMAL -> BlurMaskFilter.Blur.NORMAL
                BlurStyle.SOLID -> BlurMaskFilter.Blur.SOLID
                BlurStyle.OUTER -> BlurMaskFilter.Blur.OUTER
                BlurStyle.INNER -> BlurMaskFilter.Blur.INNER
            }
            return Android(BlurMaskFilter(radius, blurType))
        }
    }
}

enum class BlurStyle {
    NORMAL, SOLID, OUTER, INNER
}

/**
 * Extension to apply a platform blur mask filter to a Compose Paint.
 */
fun Paint.setPlatformMaskFilter(filter: PlatformBlurMaskFilter?) {
    val frameworkPaint = this.asFrameworkPaint()
    frameworkPaint.maskFilter = when (filter) {
        is PlatformBlurMaskFilter.Android -> filter.maskFilter
        PlatformBlurMaskFilter.None, null -> null
    }
}

sealed class PlatformRenderEffect {
    data class Android(val renderEffect: RenderEffect) : PlatformRenderEffect()
    data object None : PlatformRenderEffect()

    companion object {
        fun blur(
            @FloatRange(from = 0.0) radius: Float,
            tileMode: Shader.TileMode = Shader.TileMode.CLAMP
        ): PlatformRenderEffect {
            if (!PlatformCapabilities.supportsBlur || radius <= 0f) return None
            val effect =
                RenderEffect.createBlurEffect(
                    radius,
                    radius,
                    tileMode
                )
            return Android(effect)
        }

        fun colorFilter(colorFilter: ColorFilter): PlatformRenderEffect {
            if (!PlatformCapabilities.supportsColorFilter) return None
            val effect = RenderEffect.createColorFilterEffect(colorFilter)
            return Android(effect)
        }

        fun colorMatrix(matrix: FloatArray): PlatformRenderEffect {
            val cf: ColorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
            return colorFilter(cf)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun runtimeShader(shader: PlatformRuntimeShader): PlatformRenderEffect {
            val androidShader = shader.android ?: return None
            val effect = RenderEffect.createRuntimeShaderEffect(androidShader, "content")
            return Android(effect)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun chain(outer: PlatformRenderEffect, inner: PlatformRenderEffect): PlatformRenderEffect {
            val outerAndroid = outer.asAndroid() ?: return inner
            val innerAndroid = inner.asAndroid() ?: return outer
            return Android(RenderEffect.createChainEffect(outerAndroid, innerAndroid))
        }
    }
}

class PlatformRuntimeShader private constructor(val android: RuntimeShader?) {
    companion object {
        fun compile(source: String): PlatformRuntimeShader {
            return if (PlatformCapabilities.supportsRuntimeShader) {
                PlatformRuntimeShader(RuntimeShader(source))
            } else {
                PlatformRuntimeShader(null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFloatUniform(name: String, vararg values: Float) {
        val shader = android ?: return
        if (values.isEmpty()) return
        if (values.size == 1) {
            shader.setFloatUniform(name, values[0])
        } else {
            shader.setFloatUniform(name, values)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setColorUniform(name: String, argb: Int) {
        android?.setColorUniform(name, argb)
    }
}

fun PlatformRenderEffect.asAndroid(): RenderEffect? =
    when (this) {
        is PlatformRenderEffect.Android -> renderEffect
        PlatformRenderEffect.None -> null
    }