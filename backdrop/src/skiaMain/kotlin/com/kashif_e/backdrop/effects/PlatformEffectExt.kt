package com.kashif_e.backdrop.effects

import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import com.kashif_e.backdrop.platform.asSkiaImageFilter
import org.jetbrains.skia.ImageFilter

/**
 * Apply a platform render effect to the backdrop effect scope.
 * This chains the effect with any existing render effect.
 * 
 * For simple filters (blur, color), we use ImageFilter.makeCompose.
 * For runtime shader filters that need to sample content, see applyRuntimeShaderEffect.
 */
internal fun BackdropEffectScope.applyPlatformEffect(effect: PlatformRenderEffect) {
    when (effect) {
        is PlatformRenderEffect.None -> return
        is PlatformRenderEffect.Skia -> {
            val currentFilter = imageFilter
            val newFilter = effect.imageFilter
            imageFilter = if (currentFilter != null) {
                ImageFilter.makeCompose(newFilter, currentFilter)
            } else {
                newFilter
            }
        }
    }
}
