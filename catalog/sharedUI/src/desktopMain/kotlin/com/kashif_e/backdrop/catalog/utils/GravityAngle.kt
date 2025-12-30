package com.kashif_e.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo
import java.awt.MouseInfo
import java.awt.Point
import kotlin.math.atan2

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun rememberGravityAngle(): State<Float> {
    val angle = remember { mutableFloatStateOf(45f) }
    val windowInfo = LocalWindowInfo.current
    
    DisposableEffect(Unit) {
        val thread = Thread {
            while (true) {
                try {
                    val mousePoint: Point? = MouseInfo.getPointerInfo()?.location
                    
                    if (mousePoint != null && windowInfo.isWindowFocused) {
                        val containerWidth = windowInfo.containerSize.width
                        val containerHeight = windowInfo.containerSize.height
                        
                        if (containerWidth > 0 && containerHeight > 0) {
                            val centerX = containerWidth / 2f
                            val centerY = containerHeight / 2f
                            val relativeX = mousePoint.x.toFloat()
                            val relativeY = mousePoint.y.toFloat()
                            val dx = relativeX - centerX
                            val dy = relativeY - centerY
                            
                            if (dx != 0f || dy != 0f) {
                                val newAngle = atan2(dy, dx) * (180f / Math.PI.toFloat())
                                val alpha = 0.3f
                                angle.floatValue = angle.floatValue * (1f - alpha) + newAngle * alpha
                            }
                        }
                    }
                    
                    Thread.sleep(16)
                } catch (e: Exception) {
                }
            }
        }
        thread.isDaemon = true
        thread.start()
        
        onDispose {
            thread.interrupt()
        }
    }
    
    return angle
}
