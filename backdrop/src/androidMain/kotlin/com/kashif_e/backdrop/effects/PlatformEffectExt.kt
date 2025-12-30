package com.kashif_e.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import com.kashif_e.backdrop.BackdropEffectScope
import com.kashif_e.backdrop.platform.PlatformRenderEffect
import com.kashif_e.backdrop.platform.asAndroid

/**
 * Apply a platform render effect to the backdrop effect scope.
 * This chains the effect with any existing render effect.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal fun BackdropEffectScope.applyPlatformEffect(effect: PlatformRenderEffect) {
    when (effect) {
        is PlatformRenderEffect.None -> return
        is PlatformRenderEffect.Android -> {
            val current = this.renderEffect?.let { PlatformRenderEffect.Android(it) }
            val merged =
                if (current != null) {
                    PlatformRenderEffect.chain(effect, current)
                } else {
                    effect
                }
            this.renderEffect = merged.asAndroid()
        }
    }
}
