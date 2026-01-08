package com.kashif_e.backdrop.catalog.camera

import org.w3c.dom.ImageData

external interface MediaStream : JsAny {
    fun getVideoTracks(): JsArray<MediaStreamTrack>
}

external interface MediaStreamTrack : JsAny {
    fun stop()
}

// Media constraints for getUserMedia
external interface MediaStreamConstraints : JsAny

// Navigator.mediaDevices
external interface MediaDevices : JsAny {
    fun getUserMedia(constraints: MediaStreamConstraints): JsAny // Returns Promise
}

// HTML Video Element
external interface HTMLVideoElementExt : JsAny {
    var width: Int
    var height: Int
}

// Canvas 2D Context
external interface CanvasRenderingContext2DExt : JsAny

// Uint8ClampedArray for pixel data
external class Uint8ClampedArray : JsAny {
    val length: Int
    operator fun get(index: Int): Int
}

class ImageDataWrapper(val imageData: org.w3c.dom.ImageData) {
    val width: Int get() = imageData.width
    val height: Int get() = imageData.height
    
    fun toByteArray(): ByteArray {
        return extractImageDataBytes(imageData)
    }
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class, ExperimentalWasmJsInterop::class)
fun extractImageDataBytes(imageData: org.w3c.dom.ImageData): ByteArray {
    val data = imageData.data
    val length = data.length
    val result = ByteArray(length)
    
    val chunkSize = 1024
    var offset = 0
    
    while (offset < length) {
        val end = minOf(offset + chunkSize, length)
        for (i in offset until end) {
            result[i] = jsGetUint8Element(data, i)
        }
        offset = end
    }
    
    return result
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsGetUint8Element(array: org.khronos.webgl.Uint8ClampedArray, index: Int): Byte = js("""
    array[index]
""")

