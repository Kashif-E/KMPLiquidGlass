package com.kashif_e.backdrop.catalog.camera

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement

fun createMediaConstraints(width: Int, height: Int): MediaStreamConstraints {
    return jsCreateConstraints(width, height) as MediaStreamConstraints
}

private fun jsCreateConstraints(width: Int, height: Int): JsAny = js("""({
    video: {
        width: { ideal: width },
        height: { ideal: height },
        facingMode: 'user'
    },
    audio: false
})""")

// Get navigator.mediaDevices
fun getMediaDevices(): MediaDevices {
    return jsGetMediaDevices() as MediaDevices
}

private fun jsGetMediaDevices(): JsAny = js("navigator.mediaDevices")

// Attach stream to video element
fun attachStreamToVideo(videoId: String, stream: MediaStream) {
    jsAttachStream(videoId, stream)
}

private fun jsAttachStream(videoId: String, stream: MediaStream): Unit = js("""
{
    const video = document.getElementById(videoId);
    if (video) {
        video.srcObject = stream;
        video.play();
    }
}
""")

// Check if video is ready (readyState >= 2 means we have current frame data)
fun isVideoReady(videoId: String): Boolean {
    return jsIsVideoReady(videoId).toBoolean()
}

private fun jsIsVideoReady(videoId: String): JsBoolean = js("""
    (function() {
        const video = document.getElementById(videoId);
        return video ? video.readyState >= 2 : false;
    })()
""")

// Draw video to canvas
fun drawVideoToCanvas(videoId: String, canvasId: String) {
    jsDrawVideoToCanvas(videoId, canvasId)
}

private fun jsDrawVideoToCanvas(videoId: String, canvasId: String): Unit = js("""
{
    const video = document.getElementById(videoId);
    const canvas = document.getElementById(canvasId);
    if (video && canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
            canvas.width = video.videoWidth || 640;
            canvas.height = video.videoHeight || 480;
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        }
    }
}
""")

// Get image data from canvas
fun getCanvasImageData(canvasId: String, width: Int, height: Int): org.w3c.dom.ImageData? {
    return jsGetImageData(canvasId, width, height) as? org.w3c.dom.ImageData
}

private fun jsGetImageData(canvasId: String, width: Int, height: Int): JsAny? = js("""
    (function() {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return null;
        const ctx = canvas.getContext('2d');
        if (!ctx) return null;
        return ctx.getImageData(0, 0, width, height);
    })()
""")

// Stop media stream
fun stopMediaStream(stream: MediaStream) {
    jsStopStream(stream)
}

private fun jsStopStream(stream: MediaStream): Unit = js("""
{
    const tracks = stream.getVideoTracks();
    tracks.forEach(track => track.stop());
}
""")

// Create hidden video element
fun createHiddenVideoElement(elementId: String) {
    jsCreateVideoElement(elementId)
}

private fun jsCreateVideoElement(elementId: String): Unit = js("""
{
    const video = document.createElement('video');
    video.id = elementId;
    video.autoplay = true;
    video.playsInline = true;
    video.style.display = 'none';
    document.body.appendChild(video);
}
""")

// Create hidden canvas element
fun createHiddenCanvasElement(elementId: String, width: Int, height: Int) {
    jsCreateCanvasElement(elementId, width, height)
}

private fun jsCreateCanvasElement(elementId: String, width: Int, height: Int): Unit = js("""
{
    const canvas = document.createElement('canvas');
    canvas.id = elementId;
    canvas.width = width;
    canvas.height = height;
    canvas.style.display = 'none';
    document.body.appendChild(canvas);
}
""")

// Remove element
fun removeElement(elementId: String) {
    jsRemoveElement(elementId)
}

private fun jsRemoveElement(elementId: String): Unit = js("""
{
    const element = document.getElementById(elementId);
    if (element && element.parentNode) {
        element.parentNode.removeChild(element);
    }
}
""")

// Console log
external object console : JsAny {
    fun log(message: String)
    fun error(message: String)
}
