package com.kashif_e.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.catch
import org.kmp.shots.k.sensor.KSensor
import org.kmp.shots.k.sensor.SensorData.Accelerometer
import org.kmp.shots.k.sensor.SensorType
import org.kmp.shots.k.sensor.SensorUpdate
import kotlin.math.PI
import kotlin.math.atan2

@Composable
actual fun rememberGravityAngle(): State<Float> {
    val gravityAngle = remember { mutableFloatStateOf(45f) }
    
    LaunchedEffect(Unit) {
        KSensor.registerSensors(
            types = listOf(SensorType.ACCELEROMETER),
            locationIntervalMillis = 16L
        )
            .catch { }
            .collect { sensorUpdate ->
                when (sensorUpdate) {
                    is SensorUpdate.Data -> {
                        val accel = sensorUpdate.data as? Accelerometer ?: return@collect
                        val alpha = 0.15f
                        val newAngle = atan2(accel.y, accel.x) * (180f / PI.toFloat())
                        gravityAngle.floatValue = gravityAngle.floatValue * (1f - alpha) + newAngle * alpha
                    }
                    is SensorUpdate.Error -> { }
                }
            }
    }
    
    return gravityAngle
}
