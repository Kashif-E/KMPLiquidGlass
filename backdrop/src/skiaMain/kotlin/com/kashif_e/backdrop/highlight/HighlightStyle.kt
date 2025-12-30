package com.kashif_e.backdrop.highlight

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import com.kashif_e.backdrop.AmbientHighlightShaderString
import com.kashif_e.backdrop.DefaultHighlightShaderString
import com.kashif_e.backdrop.platform.PlatformRuntimeShader
import org.jetbrains.skia.Shader
import kotlin.math.PI
import kotlin.math.min

/**
 * Style configuration for highlights.
 * On iOS, shader-based styles use Skia's RuntimeEffect for SkSL shaders.
 */
@Immutable
actual sealed interface HighlightStyle {

    actual val color: Color

    actual val blendMode: BlendMode
    
    /**
     * Create a Skia shader for this highlight style.
     * Returns null if the style doesn't use a shader (e.g., Plain).
     */
    fun DrawScope.createShader(
        shape: Shape,
        shaderCache: MutableMap<String, PlatformRuntimeShader>
    ): Shader?

    /**
     * Simple plain-colored highlight stroke.
     */
    @Immutable
    data class Plain(
        override val color: Color = Color.White.copy(alpha = 0.38f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : HighlightStyle {
        override fun DrawScope.createShader(
            shape: Shape,
            shaderCache: MutableMap<String, PlatformRuntimeShader>
        ): Shader? = null
    }

    /**
     * Default gradient-based highlight.
     * Uses Skia RuntimeShader for SkSL-based rendering.
     */
    @Immutable
    data class Default(
        val intensity: Float = 0.5f,
        val angle: Float = 45f,
        val falloff: Float = 1f
    ) : HighlightStyle {
        override val color: Color = Color.White.copy(alpha = intensity)
        override val blendMode: BlendMode = BlendMode.Plus
        
        override fun DrawScope.createShader(
            shape: Shape,
            shaderCache: MutableMap<String, PlatformRuntimeShader>
        ): Shader? {
            val shader = shaderCache.getOrPut("Default") {
                PlatformRuntimeShader.compile(DefaultHighlightShaderString)
            }
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("cornerRadii", *getCornerRadii(shape))
            shader.setFloatUniform("angle", angle * (PI / 180f).toFloat())
            shader.setFloatUniform("falloff", falloff)
            return shader.build()
        }
    }

    /**
     * Ambient light highlight style.
     * Uses Skia RuntimeShader for SkSL-based rendering.
     */
    @Immutable
    data class Ambient(
        val intensity: Float = 0.38f
    ) : HighlightStyle {
        override val color: Color = Color.White.copy(alpha = intensity)
        override val blendMode: BlendMode = DrawScope.DefaultBlendMode
        
        override fun DrawScope.createShader(
            shape: Shape,
            shaderCache: MutableMap<String, PlatformRuntimeShader>
        ): Shader? {
            val shader = shaderCache.getOrPut("Ambient") {
                PlatformRuntimeShader.compile(AmbientHighlightShaderString)
            }
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("cornerRadii", *getCornerRadii(shape))
            shader.setFloatUniform("angle", 45f * (PI / 180f).toFloat())
            shader.setFloatUniform("falloff", 1f)
            return shader.build()
        }
    }

    actual companion object {

        @Stable
        actual val Default: HighlightStyle = Default()

        @Stable
        actual val Ambient: HighlightStyle = Ambient()

        @Stable
        actual val Plain: HighlightStyle = Plain()
    }
}

private fun DrawScope.getCornerRadii(shape: Shape): FloatArray {
    val size = size
    val maxRadius = size.minDimension / 2f
    val cornerShape = shape as? CornerBasedShape ?: return FloatArray(4) { maxRadius }
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft =
        if (isLtr) cornerShape.topStart.toPx(size, this)
        else cornerShape.topEnd.toPx(size, this)
    val topRight =
        if (isLtr) cornerShape.topEnd.toPx(size, this)
        else cornerShape.topStart.toPx(size, this)
    val bottomRight =
        if (isLtr) cornerShape.bottomEnd.toPx(size, this)
        else cornerShape.bottomStart.toPx(size, this)
    val bottomLeft =
        if (isLtr) cornerShape.bottomStart.toPx(size, this)
        else cornerShape.bottomEnd.toPx(size, this)
    return floatArrayOf(
        min(topLeft, maxRadius),
        min(topRight, maxRadius),
        min(bottomRight, maxRadius),
        min(bottomLeft, maxRadius)
    )
}
