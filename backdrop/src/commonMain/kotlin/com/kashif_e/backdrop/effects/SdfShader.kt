package com.kashif_e.backdrop.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * SDF (Signed Distance Field) shader for advanced glass texture effects.
 * 
 * The SDF shader uses a pre-computed distance field texture to create
 * realistic glass refraction effects. The texture encodes:
 * - R channel: Distance to nearest edge (0.5 = on edge, <0.5 = inside, >0.5 = outside)
 * - G channel: Gradient direction X for refraction
 * - B channel: Gradient direction Y for refraction
 * - A channel: Alpha/mask
 * 
 * Platform differences:
 * - Android (API 33+): Full AGSL RuntimeShader with texture sampling
 * - Desktop/iOS/Web: SkSL RuntimeEffect with Skia ImageFilter
 * 
 * @see rememberSdfShader
 */
expect class SdfShader {
    /**
     * The width of the SDF texture.
     */
    val width: Int
    
    /**
     * The height of the SDF texture.
     */
    val height: Int
}

/**
 * Remember an SdfShader instance for the given ImageBitmap.
 * 
 * The SDF texture should be a grayscale image where:
 * - R channel encodes distance from shape edges
 * - GB channels encode surface normals for refraction direction
 * - A channel provides alpha masking
 * 
 * Example usage:
 * ```kotlin
 * val sdfBitmap = imageResource(Res.drawable.sdf_texture)
 * val sdfShader = rememberSdfShader(sdfBitmap)
 * 
 * Modifier.drawPlainBackdrop(
 *     backdrop = backdrop,
 *     shape = { ContinuousRectangle },
 *     effects = {
 *         blur(2f.dp.toPx())
 *         with(sdfShader) { apply() }
 *     }
 * )
 * ```
 * 
 * @param imageBitmap The ImageBitmap containing the SDF texture
 * @return A remembered SdfShader instance
 */
@Composable
expect fun rememberSdfShader(imageBitmap: ImageBitmap): SdfShader
