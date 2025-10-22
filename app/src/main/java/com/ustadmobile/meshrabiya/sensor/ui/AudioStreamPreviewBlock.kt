package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ustadmobile.meshrabiya.sensor.ui.SensorInfo
import com.ustadmobile.meshrabiya.sensor.ui.AudioStreamLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun AudioStreamPreviewBlock(sensor: SensorInfo, onSensorChange: (SensorInfo) -> Unit) {
    // Simulate audio level state (replace with real audio input in production)
    var audioLevel by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Simulate audio level changes for preview
    LaunchedEffect(sensor.id) {
        scope.launch {
            while (true) {
                audioLevel = (10..100).random().toFloat()
                kotlinx.coroutines.delay(500)
            }
        }
    }

    AudioStreamLayout(
        audioLevel = audioLevel,
        modifier = Modifier
    )
}
