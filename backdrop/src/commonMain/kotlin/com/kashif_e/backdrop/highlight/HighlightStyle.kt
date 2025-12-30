package com.kashif_e.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * Defines the visual style for highlights.
 * 
 * Platform implementations may use different rendering techniques:
 * - Android: Can use RuntimeShader for gradient-based highlights
 * - iOS: Uses Skia for similar effects
 */
@Immutable
expect sealed interface HighlightStyle {

    val color: Color

    val blendMode: BlendMode

    companion object {
        /**
         * Default gradient-based highlight style.
         */
        @Stable
        val Default: HighlightStyle

        /**
         * Ambient lighting highlight style.
         */
        @Stable
        val Ambient: HighlightStyle

        /**
         * Simple plain-colored highlight style.
         */
        @Stable
        val Plain: HighlightStyle
    }
}
