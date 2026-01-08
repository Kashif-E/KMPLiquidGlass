package com.kashif_e.backdrop.catalog.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetMedium
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.position
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBaseAddressOfPlane
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferGetBytesPerRowOfPlane
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetHeightOfPlane
import platform.CoreVideo.CVPixelBufferGetPixelFormatType
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferGetWidthOfPlane
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onCameraReady: () -> Unit
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    
    val cameraManager = remember {
        IOSCameraManager { frame ->
            currentFrame = frame
        }
    }
    
    DisposableEffect(cameraManager) {
        cameraManager.startCamera()
        onCameraReady()
        
        onDispose {
            cameraManager.stopCamera()
        }
    }
    
    // Display current frame as Image composable
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
                text = "Starting camera...",
                color = Color.White
            )
        }
    }
}

actual fun hasCameraPermission(): Boolean {
    return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> true
        else -> false
    }
}

@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> onGranted()
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_get_main_queue().let {
                        if (granted) onGranted() else onDenied()
                    }
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> onDenied()
            else -> onDenied()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSCameraManager(
    private val onFrameReady: (ImageBitmap) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    
    private var captureSession: AVCaptureSession? = null
    private var videoOutput: AVCaptureVideoDataOutput? = null
    private var deviceInput: AVCaptureDeviceInput? = null
    
    private val sessionQueue = dispatch_queue_create("com.backdrop.sessionQueue", null)
    private val videoQueue = dispatch_queue_create("com.backdrop.videoQueue", null)
    
    fun startCamera() {
        platform.darwin.dispatch_async(sessionQueue) {
            setupAndStartSession()
        }
    }
    
    private fun setupAndStartSession() {
        val session = AVCaptureSession()
        
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        
        val input = try {
            AVCaptureDeviceInput.deviceInputWithDevice(device, null)
        } catch (e: Exception) {
            return
        } ?: return
        
        deviceInput = input
        
        val output = AVCaptureVideoDataOutput()
        videoOutput = output
        
        output.videoSettings = mapOf(
            kCVPixelBufferPixelFormatTypeKey to NSNumber(kCVPixelFormatType_32BGRA.toInt())
        )
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(this, videoQueue)
        
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetMedium
        
        if (session.canAddInput(input)) {
            session.addInput(input)
        } else {
            session.commitConfiguration()
            return
        }
        
        if (session.canAddOutput(output)) {
            session.addOutput(output)
        } else {
            session.commitConfiguration()
            return
        }
        
        val connection = output.connectionWithMediaType(AVMediaTypeVideo)
        connection?.setEnabled(true)
        
        session.commitConfiguration()
        captureSession = session
        session.startRunning()
    }
    
    fun stopCamera() {
        platform.darwin.dispatch_async(sessionQueue) {
            captureSession?.stopRunning()
            captureSession = null
            videoOutput = null
            deviceInput = null
        }
    }
    
    override fun captureOutput(
        output: platform.AVFoundation.AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        val sampleBuffer = didOutputSampleBuffer ?: return
        val imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return
        
        CVPixelBufferLockBaseAddress(imageBuffer, 0u)
        
        try {
            val width = CVPixelBufferGetWidth(imageBuffer).toInt()
            val height = CVPixelBufferGetHeight(imageBuffer).toInt()
            val pixelFormat = platform.CoreVideo.CVPixelBufferGetPixelFormatType(imageBuffer)
            
            if (width <= 0 || height <= 0) return
            
            val formatUInt = pixelFormat.toUInt()
            val rgbaData = when {
                formatUInt == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange || 
                formatUInt == kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange -> {
                    convertYUVtoRGBA(imageBuffer, width, height, formatUInt == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)
                }
                formatUInt == kCVPixelFormatType_32BGRA -> {
                    convertBGRAtoRGBA(imageBuffer, width, height)
                }
                else -> return
            } ?: return
            
            val imageInfo = ImageInfo(
                width = width,
                height = height,
                colorType = ColorType.RGBA_8888,
                alphaType = ColorAlphaType.OPAQUE
            )
            
            val skiaImage = org.jetbrains.skia.Image.makeRaster(
                imageInfo,
                rgbaData,
                width * 4
            )
            
            val imageBitmap = skiaImage.toComposeImageBitmap()
            
            platform.darwin.dispatch_async(dispatch_get_main_queue()) {
                onFrameReady(imageBitmap)
            }
        } catch (e: Exception) {
            // Ignore frame processing errors
        } finally {
            CVPixelBufferUnlockBaseAddress(imageBuffer, 0u)
        }
    }
    
    private fun convertYUVtoRGBA(
        pixelBuffer: platform.CoreVideo.CVPixelBufferRef, 
        width: Int, 
        height: Int,
        isFullRange: Boolean = false
    ): ByteArray? {
        val yPlane = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0u) ?: return null
        val uvPlane = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 1u) ?: return null
        
        val yBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 0u).toInt()
        val uvBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 1u).toInt()
        
        val yBuffer = yPlane.reinterpret<ByteVar>()
        val uvBuffer = uvPlane.reinterpret<ByteVar>()
        
        val rgbaData = ByteArray(width * height * 4)
        
        for (row in 0 until height) {
            for (col in 0 until width) {
                val yIndex = row * yBytesPerRow + col
                val uvIndex = (row / 2) * uvBytesPerRow + (col / 2) * 2
                
                val y = yBuffer[yIndex].toInt() and 0xFF
                val u = uvBuffer[uvIndex].toInt() and 0xFF
                val v = uvBuffer[uvIndex + 1].toInt() and 0xFF
                
                val r: Int
                val g: Int
                val b: Int
                
                if (isFullRange) {
                    val c = y - 16
                    val d = u - 128
                    val e = v - 128
                    
                    r = ((298 * c + 409 * e + 128) shr 8).coerceIn(0, 255)
                    g = ((298 * c - 100 * d - 208 * e + 128) shr 8).coerceIn(0, 255)
                    b = ((298 * c + 516 * d + 128) shr 8).coerceIn(0, 255)
                } else {
                    val yNorm = (y - 16).coerceIn(0, 219)
                    val uNorm = u - 128
                    val vNorm = v - 128
                    
                    r = ((1164 * yNorm + 1596 * vNorm) / 1000).coerceIn(0, 255)
                    g = ((1164 * yNorm - 391 * uNorm - 813 * vNorm) / 1000).coerceIn(0, 255)
                    b = ((1164 * yNorm + 2018 * uNorm) / 1000).coerceIn(0, 255)
                }
                
                val pixelIndex = (row * width + col) * 4
                rgbaData[pixelIndex] = r.toByte()
                rgbaData[pixelIndex + 1] = g.toByte()
                rgbaData[pixelIndex + 2] = b.toByte()
                rgbaData[pixelIndex + 3] = 255.toByte()
            }
        }
        
        return rgbaData
    }
    
    private fun convertBGRAtoRGBA(
        pixelBuffer: platform.CoreVideo.CVPixelBufferRef, 
        width: Int, 
        height: Int
    ): ByteArray? {
        val baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer) ?: return null
        val bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer).toInt()
        val bytePtr = baseAddress.reinterpret<ByteVar>()
        
        val rgbaData = ByteArray(width * height * 4)
        
        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcIndex = row * bytesPerRow + col * 4
                val dstIndex = (row * width + col) * 4
                
                rgbaData[dstIndex] = bytePtr[srcIndex + 2]
                rgbaData[dstIndex + 1] = bytePtr[srcIndex + 1]
                rgbaData[dstIndex + 2] = bytePtr[srcIndex]
                rgbaData[dstIndex + 3] = bytePtr[srcIndex + 3]
            }
        }
        
        return rgbaData
    }
}