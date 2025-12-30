package com.kashif_e.backdrop.effects

import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import com.kashif_e.backdrop.platform.asSkiaImageFilter
import org.jetbrains.skia.ImageFilter

/**
 * Apply an ImageFilter effect directly to the backdrop.
 */
fun BackdropEffectScope.effect(effect: ImageFilter) {
    applyEffect(PlatformRenderEffect.Skia(effect))
}

/**
 * Apply a PlatformRenderEffect to the backdrop.
 */
fun BackdropEffectScope.effect(effect: PlatformRenderEffect) {
    applyEffect(effect)
}
