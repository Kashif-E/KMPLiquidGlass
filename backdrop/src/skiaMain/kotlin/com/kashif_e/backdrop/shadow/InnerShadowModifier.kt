package com.kashif_e.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import com.kashif_e.backdrop.ShapeProvider
import com.kashif_e.backdrop.clipOutline
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter

internal class InnerShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> InnerShadow?
) : ModifierNodeElement<InnerShadowNode>() {

    override fun create(): InnerShadowNode {
        return InnerShadowNode(shapeProvider, shadow)
    }

    override fun update(node: InnerShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false

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

internal class InnerShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> InnerShadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private val maskPaint = Paint().apply {
        blendMode = BlendMode.Clear
    }
    private var clipPath: Path? = null

    private var prevRadius = Float.NaN
    private var currentImageFilter: ImageFilter? = null

    override fun ContentDrawScope.draw() {
        drawContent()

        val shadow = shadow() ?: return

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()
            val offsetX = shadow.offset.x.toPx()
            val offsetY = shadow.offset.y.toPx()

            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)
            val clipPath =
                if (outline is Outline.Rounded) {
                    clipPath ?: Path().also { clipPath = it }
                } else {
                    null
                }

            configurePaint(shadow)

            // Update blur filter if radius changed
            if (prevRadius != radius) {
                currentImageFilter = if (radius > 0f) {
                    val sigma = radius / 2f
                    ImageFilter.makeBlur(sigma, sigma, FilterTileMode.DECAL)
                } else {
                    null
                }
                prevRadius = radius
            }

            shadowLayer.alpha = shadow.alpha
            shadowLayer.blendMode = shadow.blendMode
            // Apply the blur filter as a RenderEffect on the graphics layer
            shadowLayer.renderEffect = currentImageFilter?.asComposeRenderEffect()
            shadowLayer.record {
                val canvas = drawContext.canvas
                canvas.save()
                canvas.clipOutline(outline, clipPath)

                // Draw shadow color fill
                drawOutline(outline, paint)

                // Draw clear mask at offset
                canvas.translate(offsetX, offsetY)
                drawOutline(outline, maskPaint)
                canvas.translate(-offsetX, -offsetY)

                canvas.restore()
            }

            // Draw the shadow layer (blur is applied via renderEffect)
            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipOutline(outline, clipPath)

            drawLayer(shadowLayer)
            canvas.restore()
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            shadowLayer = null
        }
        currentImageFilter = null
    }

    private fun DrawScope.configurePaint(shadow: InnerShadow) {
        paint.color = shadow.color
    }

    private fun DrawScope.drawOutline(outline: Outline, paint: Paint) {
        when (outline) {
            is Outline.Rectangle -> drawContext.canvas.drawRect(outline.rect, paint)
            is Outline.Rounded -> {
                val path = clipPath ?: Path()
                path.reset()
                path.addRoundRect(outline.roundRect)
                drawContext.canvas.drawPath(path, paint)
            }
            is Outline.Generic -> drawContext.canvas.drawPath(outline.path, paint)
        }
    }
}
