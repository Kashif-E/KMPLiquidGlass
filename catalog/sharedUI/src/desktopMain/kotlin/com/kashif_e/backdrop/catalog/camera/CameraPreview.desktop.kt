package com.kashif_e.backdrop.catalog.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.github.sarxos.webcam.Webcam
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Dimension
import java.awt.image.BufferedImage

private val isNativeDriverInitialized by lazy {
    try {
        Webcam.setDriver(NativeDriver())
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onCameraReady: () -> Unit
) {
    remember { isNativeDriverInitialized }
    
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val cameraManager = remember {
        DesktopCameraManager { frame ->
            currentFrame = frame
        }
    }
    
    DisposableEffect(cameraManager) {
        scope.launch(Dispatchers.IO) {
            if (cameraManager.startCamera()) {
                onCameraReady()
            } else {
                errorMessage = "No webcam found"
            }
        }
        
        onDispose {
            cameraManager.stopCamera()
        }
    }
    
    val frame = currentFrame
    if (frame != null) {
        Image(
            bitmap = frame,
            contentDescription = "Camera Preview",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage ?: "Starting camera...",
                color = Color.White
            )
        }
    }
}

actual fun hasCameraPermission(): Boolean {
    return true
}

@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    LaunchedEffect(Unit) {
        onGranted()
    }
}

private class DesktopCameraManager(
    private val onFrameReady: (ImageBitmap) -> Unit
) {
    private var webcam: Webcam? = null
    private var isRunning = false
    
    fun startCamera(): Boolean {
        try {
            val availableWebcam = Webcam.getDefault() ?: return false
            
            availableWebcam.viewSize = Dimension(640, 480)
            availableWebcam.open()
            
            webcam = availableWebcam
            isRunning = true
            
            Thread {
                while (isRunning) {
                    try {
                        val image = availableWebcam.image
                        if (image != null) {
                            val imageBitmap = convertBufferedImageToImageBitmap(image)
                            onFrameReady(imageBitmap)
                        }
                        Thread.sleep(33) // ~30 fps
                    } catch (e: Exception) {
                        if (isRunning) {
                            e.printStackTrace()
                        }
                    }
                }
            }.start()
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun stopCamera() {
        isRunning = false
        try {
            webcam?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webcam = null
    }
    
    private fun convertBufferedImageToImageBitmap(bufferedImage: BufferedImage): ImageBitmap {
        val width = bufferedImage.width
        val height = bufferedImage.height
        
        val rgbaData = ByteArray(width * height * 4)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = bufferedImage.getRGB(x, y)
                val index = (y * width + x) * 4
                
                rgbaData[index] = ((rgb shr 16) and 0xFF).toByte()     // R
                rgbaData[index + 1] = ((rgb shr 8) and 0xFF).toByte()  // G
                rgbaData[index + 2] = (rgb and 0xFF).toByte()          // B
                rgbaData[index + 3] = ((rgb shr 24) and 0xFF).toByte() // A
            }
        }
        
        val imageInfo = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.RGBA_8888,
            alphaType = ColorAlphaType.UNPREMUL
        )
        
        val skiaImage = org.jetbrains.skia.Image.makeRaster(
            imageInfo,
            rgbaData,
            width * 4
        )
        
        return skiaImage.toComposeImageBitmap()
    }
}
