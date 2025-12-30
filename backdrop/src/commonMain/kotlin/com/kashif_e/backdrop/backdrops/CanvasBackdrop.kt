package com.kashif_e.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kashif_e.backdrop.Backdrop

/**
 * Creates a backdrop that draws custom content via a DrawScope lambda.
 * 
 * @param onDraw The drawing lambda that will be called to render the backdrop
 */
@Composable
fun rememberCanvasBackdrop(
    onDraw: DrawScope.() -> Unit
): Backdrop {
    return remember(onDraw) {
        CanvasBackdrop(onDraw)
    }
}

/**
 * Factory function to create a CanvasBackdrop without @Composable context.
 * 
 * @param onDraw The drawing lambda that will be called to render the backdrop
 */
fun CanvasBackdrop(
    onDraw: DrawScope.() -> Unit
): Backdrop = CanvasBackdropImpl(onDraw)

@Immutable
private class CanvasBackdropImpl(
    val onDraw: DrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw()
    }
}
