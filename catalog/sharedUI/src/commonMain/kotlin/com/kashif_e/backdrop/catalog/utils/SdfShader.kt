package com.kashif_e.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource

/**
 * SDF (Signed Distance Field) shader for advanced glass texture effects.
 * 
 * The SDF shader uses a pre-computed distance field texture to create
 * realistic glass refraction effects. The texture encodes:
 * - R channel: Distance to nearest edge (0.5 = on edge, <0.5 = inside, >0.5 = outside)
 * - G channel: Gradient direction for refraction
 * 
 * Platform differences:
 * - Android (API 33+): Full AGSL RuntimeShader with texture sampling
 * - Desktop/iOS/Web: SkSL RuntimeEffect with simplified refraction
 * 
 * @see rememberSdfShader
 */
expect class SdfShader

/**
 * Remember an SdfShader instance for the given drawable resource.
 * 
 * The SDF texture should be a grayscale image where:
 * - Lighter areas indicate distance from shape edges
 * - The gradient encodes surface normals for refraction direction
 * 
 * Example usage:
 * ```kotlin
 * val sdfShader = rememberSdfShader(Res.drawable.sdf_texture)
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
 * @param resource The drawable resource containing the SDF texture
 * @return A remembered SdfShader instance
 */
@Composable
expect fun rememberSdfShader(resource: DrawableResource): SdfShader
