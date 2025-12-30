package com.kashif_e.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for highlight effects that create glossy glass-like appearances.
 * 
 * Highlights add a subtle stroke around the shape that simulates light reflection,
 * making glass effects look more realistic.
 * 
 * @param width The width of the highlight stroke.
 * @param blurRadius The blur radius for soft highlights.
 * @param alpha The opacity of the highlight.
 * @param style The style of the highlight (Default, Ambient, or Plain).
 */
@Immutable
data class Highlight(
    val width: Dp = 0.5f.dp,
    val blurRadius: Dp = width / 2f,
    val alpha: Float = 1f,
    val style: HighlightStyle = HighlightStyle.Default
) {

    companion object {

        /**
         * Default highlight with gradient-based shading.
         */
        @Stable
        val Default: Highlight = Highlight()

        /**
         * Ambient highlight that simulates environmental lighting.
         */
        @Stable
        val Ambient: Highlight = Highlight(style = HighlightStyle.Ambient)

        /**
         * Simple plain-colored highlight without gradient effects.
         */
        @Stable
        val Plain: Highlight = Highlight(style = HighlightStyle.Plain)
    }
}
