package com.kashif_e.backdrop

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

/**
 * Interface for backdrop content providers.
 * Implementations define what gets drawn as the backdrop content
 * before effects are applied.
 */
interface Backdrop {

    /**
     * Whether this backdrop depends on layout coordinates for positioning.
     * When true, the backdrop will be re-rendered when coordinates change.
     */
    val isCoordinatesDependent: Boolean

    /**
     * Draw the backdrop content.
     * 
     * @param density The current density for dp/px conversion
     * @param coordinates The layout coordinates of the backdrop component
     * @param layerBlock Optional graphics layer configuration
     */
    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
