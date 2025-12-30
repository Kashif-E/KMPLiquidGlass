package com.kashif_e.backdrop.catalog.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
}
