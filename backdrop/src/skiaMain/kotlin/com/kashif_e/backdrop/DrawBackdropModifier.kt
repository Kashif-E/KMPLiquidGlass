package com.kashif_e.backdrop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Outline
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.highlight.HighlightElement
import com.kashif_e.backdrop.shadow.InnerShadow
import com.kashif_e.backdrop.shadow.InnerShadowElement
import com.kashif_e.backdrop.shadow.Shadow
import com.kashif_e.backdrop.shadow.ShadowElement

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

/**
 * Apply a plain backdrop effect without shadow/highlight decorations.
 */
actual fun Modifier.drawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)?,
    exportedBackdrop: LayerBackdrop?,
    onDrawBehind: (DrawScope.() -> Unit)?,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)?,
    onDrawSurface: (DrawScope.() -> Unit)?,
    onDrawFront: (DrawScope.() -> Unit)?
): Modifier {
    val shapeProvider = ShapeProvider(shape)
    val actualOnDrawBackdrop = onDrawBackdrop ?: DefaultOnDrawBackdrop
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = actualOnDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront
            )
        )
}

private val DefaultHighlight = { Highlight.Default }
private val DefaultShadow = { Shadow.Default }

/**
 * Apply a backdrop effect with optional shadow/highlight.
 */
actual fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)?,
    shadow: (() -> Shadow?)?,
    innerShadow: (() -> InnerShadow?)?,
    layerBlock: (GraphicsLayerScope.() -> Unit)?,
    exportedBackdrop: LayerBackdrop?,
    onDrawBehind: (DrawScope.() -> Unit)?,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)?,
    onDrawSurface: (DrawScope.() -> Unit)?,
    onDrawFront: (DrawScope.() -> Unit)?
): Modifier {
    val shapeProvider = ShapeProvider(shape)
    val actualOnDrawBackdrop = onDrawBackdrop ?: DefaultOnDrawBackdrop
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            if (innerShadow != null) {
                InnerShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = innerShadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (shadow != null) {
                ShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = shadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (highlight != null) {
                HighlightElement(
                    shapeProvider = shapeProvider,
                    highlight = highlight
                )
            } else {
                Modifier
            }
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = actualOnDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront
            )
        )
}

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shapeProvider: ShapeProvider,
    val effects: BackdropEffectScope.() -> Unit,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode {
        return DrawBackdropNode(
            backdrop = backdrop,
            shapeProvider = shapeProvider,
            effects = effects,
            layerBlock = layerBlock,
            onDrawBehind = onDrawBehind,
            onDrawBackdrop = onDrawBackdrop,
            onDrawSurface = onDrawSurface,
            onDrawFront = onDrawFront
        )
    }

    override fun update(node: DrawBackdropNode) {
        node.backdrop = backdrop
        node.shapeProvider = shapeProvider
        node.effects = effects
        node.layerBlock = layerBlock
        node.onDrawBehind = onDrawBehind
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["shapeProvider"] = shapeProvider
        properties["effects"] = effects
        properties["layerBlock"] = layerBlock
        properties["onDrawBehind"] = onDrawBehind
        properties["onDrawBackdrop"] = onDrawBackdrop
        properties["onDrawSurface"] = onDrawSurface
        properties["onDrawFront"] = onDrawFront
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (shapeProvider != other.shapeProvider) return false
        if (effects != other.effects) return false
        if (layerBlock != other.layerBlock) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (onDrawFront != other.onDrawFront) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shapeProvider.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + onDrawBackdrop.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + (onDrawFront?.hashCode() ?: 0)
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shapeProvider: ShapeProvider,
    var effects: BackdropEffectScope.() -> Unit,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?
) : LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private val effectScope = object : BackdropEffectScopeImpl() {
        override val shape: Shape get() = shapeProvider.innerShape
    }

    private var graphicsLayer: GraphicsLayer? = null

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = CompositingStrategy.Offscreen
    }

    private var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    private var padding by mutableFloatStateOf(0f)
    
    // Path for clipping the backdrop to shape
    private var clipPath: Path? = null

    private val recordBackdropBlock: (DrawScope.() -> Unit) = {
        val canvas = drawContext.canvas
        val currentPadding = padding

        if (currentPadding != 0f) {
            canvas.translate(currentPadding, currentPadding)
        }
        onDrawBackdrop {
            with(backdrop) {
                drawBackdrop(
                    density = effectScope,
                    coordinates = layoutCoordinates,
                    layerBlock = layerBlock
                )
            }
        }
        if (currentPadding != 0f) {
            canvas.translate(-currentPadding, -currentPadding)
        }
    }

    private val drawBackdropLayer: DrawScope.() -> Unit = {
        val layer = graphicsLayer
        if (layer != null) {
            val currentPadding = padding
            val currentSize = size

            with(this@DrawBackdropNode) {
                recordLayer(
                    layer,
                    size = IntSize(
                        currentSize.width.toInt() + currentPadding.toInt() * 2,
                        currentSize.height.toInt() + currentPadding.toInt() * 2
                    ),
                    block = recordBackdropBlock
                )
            }

            // Clip to shape before drawing the layer with effects
            val canvas = drawContext.canvas
            val outline = shapeProvider.shape.createOutline(currentSize, layoutDirection, this)
            val path = clipPath ?: Path().also { clipPath = it }
            
            canvas.save()
            canvas.clipOutline(outline, path)
            
            layer.topLeft =
                if (currentPadding != 0f) IntOffset(-currentPadding.toInt(), -currentPadding.toInt())
                else IntOffset.Zero
            drawLayer(layer)
            
            canvas.restore()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (effectScope.update(this)) {
            updateEffects()
        }

        onDrawBehind?.invoke(this)
        drawBackdropLayer()
        onDrawSurface?.invoke(this)
        drawContent()
        onDrawFront?.invoke(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (backdrop.isCoordinatesDependent) {
                layoutCoordinates = coordinates
            } else {
                if (layoutCoordinates != null) {
                    layoutCoordinates = null
                }
            }
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        observeEffects()
    }

    private fun observeEffects() {
        observeReads { updateEffects() }
    }

    private fun updateEffects() {
        effectScope.apply(effects)
        // On iOS/Skia, convert the ImageFilter to a Compose RenderEffect
        graphicsLayer?.renderEffect = effectScope.imageFilter?.asComposeRenderEffect()
        padding = effectScope.padding
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer = graphicsContext.createGraphicsLayer()

        observeEffects()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }

        effectScope.reset()
        layoutCoordinates = null
        clipPath = null
    }
}
