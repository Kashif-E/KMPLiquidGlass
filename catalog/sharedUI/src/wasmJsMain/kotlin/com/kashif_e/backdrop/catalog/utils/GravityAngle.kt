package com.kashif_e.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.math.PI
import kotlin.math.atan2

@Composable
actual fun rememberGravityAngle(): State<Float> {
    val angle = remember { mutableFloatStateOf(45f) }
    
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val mouseEvent = event as? org.w3c.dom.events.MouseEvent
            if (mouseEvent != null) {
                val centerX = window.innerWidth / 2.0
                val centerY = window.innerHeight / 2.0
                val dx = mouseEvent.clientX - centerX
                val dy = mouseEvent.clientY - centerY
                
                if (dx != 0.0 || dy != 0.0) {
                    val newAngle = (atan2(dy, dx) * (180.0 / PI)).toFloat()
                    val alpha = 0.3f
                    angle.floatValue = angle.floatValue * (1f - alpha) + newAngle * alpha
                }
            }
        }
        
        document.addEventListener("mousemove", listener)
        
        onDispose {
            document.removeEventListener("mousemove", listener)
        }
    }
    
    return angle
}
