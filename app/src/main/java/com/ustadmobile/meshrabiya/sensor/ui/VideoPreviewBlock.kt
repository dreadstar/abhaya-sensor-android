package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.ustadmobile.meshrabiya.sensor.ui.SensorInfo
import com.ustadmobile.meshrabiya.sensor.ui.VideoLayout
import com.ustadmobile.meshrabiya.sensor.capture.CameraControllerImpl
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.camera.view.PreviewView
import com.ustadmobile.meshrabiya.sensor.camera.CameraController

@Composable
fun VideoPreviewBlock(sensor: SensorInfo, onSensorChange: (SensorInfo) -> Unit,cameraController: CameraController) {
    // Use videoUri from SensorInfo if available, else simulate
    val videoUri = sensor.videoUri ?: "simulated_video_uri"
    val type = if (sensor.type.isNotEmpty()) sensor.type else "camera"
    // Pass required props to VideoLayout (simulate CameraController and other controls)
    // val cameraController = remember { com.ustadmobile.meshrabiya.sensor.camera.CameraControllerImpl() }
    var videoFrameRate by remember { mutableStateOf(30) }
    var videoFormat by remember { mutableStateOf("H.264") }
    val selectedSensors = remember { setOf(sensor.id) }
    VideoLayout(
        cameraController = cameraController,
        videoFrameRate = videoFrameRate,
        onFrameRateChange = { videoFrameRate = it },
        videoFormat = videoFormat,
        onFormatChange = { videoFormat = it },
        selectedSensors = selectedSensors,
        modifier = Modifier
    )
}
