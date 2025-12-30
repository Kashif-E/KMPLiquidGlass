package com.kashif_e.backdrop

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kashif_e.backdrop.platform.PlatformRuntimeShader
import org.jetbrains.skia.ImageFilter

/**
 * iOS/Skia-specific BackdropEffectScope that provides access to
 * Skia's ImageFilter APIs.
 */
actual sealed interface BackdropEffectScope : Density {

    actual val size: Size

    actual val layoutDirection: LayoutDirection

    actual val shape: Shape

    actual var padding: Float

    /**
     * The Skia ImageFilter to apply to the backdrop.
     */
    var imageFilter: ImageFilter?
    
    /**
     * Obtain a cached runtime shader for the given key and source.
     */
    fun obtainPlatformShader(key: String, source: String): PlatformRuntimeShader
}

internal abstract class BackdropEffectScopeImpl : BackdropEffectScope {

    override var density: Float = 1f
    override var fontScale: Float = 1f
    override var size: Size = Size.Unspecified
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var padding: Float = 0f
    override var imageFilter: ImageFilter? = null

    private val shaderCache = mutableMapOf<String, PlatformRuntimeShader>()

    override fun obtainPlatformShader(key: String, source: String): PlatformRuntimeShader {
        return shaderCache.getOrPut(key) {
            PlatformRuntimeShader.compile(source)
        }
    }

    fun update(scope: DrawScope): Boolean {
        val newDensity = scope.density
        val newFontScale = scope.fontScale
        val newSize = scope.size
        val newLayoutDirection = scope.layoutDirection

        val changed = newDensity != density ||
                newFontScale != fontScale ||
                newSize != size ||
                newLayoutDirection != layoutDirection

        if (changed) {
            density = newDensity
            fontScale = newFontScale
            size = newSize
            layoutDirection = newLayoutDirection
        }

        return changed
    }

    fun apply(effects: BackdropEffectScope.() -> Unit) {
        padding = 0f
        imageFilter = null
        effects()
    }

    fun reset() {
        density = 1f
        fontScale = 1f
        size = Size.Unspecified
        layoutDirection = LayoutDirection.Ltr
        padding = 0f
        imageFilter = null
        shaderCache.clear()
    }
}
