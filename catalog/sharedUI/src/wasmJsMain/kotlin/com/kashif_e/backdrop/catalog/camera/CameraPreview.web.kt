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
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.random.Random

@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onCameraReady: () -> Unit
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val cameraManager = remember {
        WebCameraManager { frame ->
            currentFrame = frame
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            cameraManager.startCamera()
            onCameraReady()
        } catch (e: Exception) {
            errorMessage = "Camera access denied"
            console.error("Camera error: ${e.message}")
        }
    }
    
    // Periodic frame capture using Kotlin coroutines (30 FPS)
    LaunchedEffect(cameraManager) {
        while (cameraManager.isRunning) {
            cameraManager.captureFrame()
            kotlinx.coroutines.delay(33)
        }
    }
    
    DisposableEffect(Unit) {
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

private class WebCameraManager(
    private val onFrameReady: (ImageBitmap) -> Unit
) {
    private var mediaStream: MediaStream? = null
    var isRunning = false
        private set
    
    private val videoElementId = "wasm-camera-video-${Random.nextInt()}"
    private val canvasElementId = "wasm-camera-canvas-${Random.nextInt()}"
    
    private val captureWidth = 320
    private val captureHeight = 240
    
    fun startCamera() {
        try {
            createHiddenVideoElement(videoElementId)
            createHiddenCanvasElement(canvasElementId, captureWidth, captureHeight)
            startCameraJS(videoElementId)
            isRunning = true
        } catch (e: Exception) {
            console.error("Camera initialization error: ${e.message}")
            cleanup()
        }
    }
    
    private fun startFrameCapture() {
        // Frame capture will be handled by Compose's LaunchedEffect
        // This is just a marker that we're ready to capture
        isRunning = true
    }
    
    fun captureFrame() {
        if (!isRunning) return
        
        try {
            if (!isVideoReady(videoElementId)) return
            
            drawVideoToCanvas(videoElementId, canvasElementId)
            
            val imageData = getCanvasImageData(canvasElementId, captureWidth, captureHeight)
            if (imageData != null) {
                val wrapper = ImageDataWrapper(imageData)
                val imageBitmap = convertToImageBitmap(wrapper)
                onFrameReady(imageBitmap)
            }
        } catch (e: Exception) {
            console.error("Frame capture error: ${e.message}")
        }
    }
    
    fun stopCamera() {
        isRunning = false
        cleanup()
    }
    
    private fun cleanup() {
        mediaStream?.let { stopMediaStream(it) }
        mediaStream = null
        removeElement(videoElementId)
        removeElement(canvasElementId)
    }
    
    private fun convertToImageBitmap(wrapper: ImageDataWrapper): ImageBitmap {
        val byteArray = wrapper.toByteArray()
        
        val imageInfo = ImageInfo(
            width = wrapper.width,
            height = wrapper.height,
            colorType = ColorType.RGBA_8888,
            alphaType = ColorAlphaType.UNPREMUL
        )
        
        val skiaImage = org.jetbrains.skia.Image.makeRaster(
            imageInfo,
            byteArray,
            wrapper.width * 4
        )
        
        return skiaImage.toComposeImageBitmap()
    }
}

// Direct JS camera start (simpler approach without Promise callback)
private fun startCameraJS(videoId: String) {
    jsStartCamera(videoId)
}

private fun jsStartCamera(videoId: String): Unit = js("""
{
    navigator.mediaDevices.getUserMedia({
        video: {
            width: { ideal: 320 },
            height: { ideal: 240 },
            facingMode: 'user'
        },
        audio: false
    }).then(function(stream) {
        const video = document.getElementById(videoId);
        if (video) {
            video.srcObject = stream;
            video.play();
        }
    }).catch(function(error) {
        console.error('getUserMedia error:', error.name, error.message);
    });
}
""")
