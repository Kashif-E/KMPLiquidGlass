package com.kashif_e.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import com.kashif_e.backdrop.ShapeProvider
import com.kashif_e.backdrop.platform.PlatformBlurMaskFilter
import com.kashif_e.backdrop.platform.setPlatformMaskFilter
import org.jetbrains.skia.PaintMode as SkiaPaintMode

internal class ShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> Shadow?
) : ModifierNodeElement<ShadowNode>() {

    override fun create(): ShadowNode {
        return ShadowNode(shapeProvider, shadow)
    }

    override fun update(node: ShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShadowElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal class ShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> Shadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private val shadowPaint = Paint()
    private val clearPaint = Paint().apply {
        blendMode = BlendMode.Clear
    }
    
    // Native Skia paint for layer alpha
    private val layerPaint = org.jetbrains.skia.Paint()

    override fun ContentDrawScope.draw() {
        val shadow = shadow() ?: return drawContent()

        val size = size
        val density: Density = this
        val layoutDirection = layoutDirection

        val radius = shadow.radius.toPx()
        val offsetX = shadow.offset.x.toPx()
        val offsetY = shadow.offset.y.toPx()
        val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)

        configurePaint(shadow)

        // Use native Skia canvas with saveLayer for proper blending
        val nativeCanvas = drawContext.canvas.nativeCanvas
        
        // Calculate bounds for the shadow layer
        val layerBounds = org.jetbrains.skia.Rect.makeLTRB(
            -radius * 2f,
            -radius * 2f,
            size.width + radius * 2f + offsetX,
            size.height + radius * 2f + offsetY
        )
        
        // Apply alpha to the layer (like Android's GraphicsLayer.alpha)
        layerPaint.alpha = (shadow.alpha * 255).toInt()
        
        // Save layer for offscreen compositing with alpha
        nativeCanvas.saveLayer(layerBounds, layerPaint)
        
        // Draw the blurred shadow at offset (full opacity within layer)
        val canvas = drawContext.canvas
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.drawOutline(outline, shadowPaint)
        canvas.restore()
        
        // Cut out the center with Clear blend mode
        canvas.drawOutline(outline, clearPaint)
        
        // Restore layer (composites with alpha)
        nativeCanvas.restore()

        drawContent()
    }

    private fun DrawScope.configurePaint(shadow: Shadow) {
        shadowPaint.color = shadow.color  // Color already contains its alpha (e.g., 0.1f for black)
        // Don't override the color's alpha - it's intentionally set low for subtle shadows
        val blurRadius = shadow.radius.toPx()
        val maskFilter = PlatformBlurMaskFilter.create(blurRadius)
        shadowPaint.setPlatformMaskFilter(maskFilter)
    }
}

private val ShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
