package com.kashif_e.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kashif_e.backdrop.Backdrop

/**
 * A backdrop that captures and renders content from a graphics layer.
 * 
 * Use this for advanced effects where you want to use one composable's
 * rendered content as the backdrop for another composable.
 * 
 * Platform-specific implementations handle the graphics layer management.
 */
@Stable
expect class LayerBackdrop : Backdrop {
    override val isCoordinatesDependent: Boolean
    
    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    )
}

/**
 * Creates and remembers a [LayerBackdrop].
 * 
 * @param onDraw Optional custom drawing logic for the layer content.
 */
@Composable
expect fun rememberLayerBackdrop(
    onDraw: ContentDrawScope.() -> Unit = { drawContent() }
): LayerBackdrop

/**
 * Modifier that captures the content of this composable into a [LayerBackdrop].
 * 
 * The captured content can then be used as a backdrop for other composables
 * using [drawBackdrop] with [rememberCombinedBackdrop].
 */
expect fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier
