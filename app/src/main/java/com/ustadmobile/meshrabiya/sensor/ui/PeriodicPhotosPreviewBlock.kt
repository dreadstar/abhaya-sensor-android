package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.ustadmobile.meshrabiya.sensor.ui.SensorInfo
import com.ustadmobile.meshrabiya.sensor.ui.PeriodicPhotosLayout
import com.ustadmobile.meshrabiya.sensor.capture.CameraControllerImpl
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.camera.view.PreviewView
import com.ustadmobile.meshrabiya.sensor.camera.CameraController

@Composable
fun PeriodicPhotosPreviewBlock(sensor: SensorInfo, onSensorChange: (SensorInfo) -> Unit, cameraController: CameraController) {
    // Use photoUris from SensorInfo if available, else simulate
    val photoUris = if (sensor.photoUris.isNotEmpty()) sensor.photoUris else listOf("simulated_photo_uri_1", "simulated_photo_uri_2")
    val type = if (sensor.type.isNotEmpty()) sensor.type else "camera"
    // val cameraController = remember { com.ustadmobile.meshrabiya.sensor.camera.CameraControllerImpl() }
    var photoFrequency by remember { mutableStateOf(5) }
    var photoAspectRatio by remember { mutableStateOf("16:9") }
    var photoPixelDepth by remember { mutableStateOf("8-bit") }
    val selectedSensors = remember { setOf(sensor.id) }
    PeriodicPhotosLayout(
        cameraController = cameraController,
        photoFrequency = photoFrequency,
        onPhotoFrequencyChange = { photoFrequency = it },
        photoAspectRatio = photoAspectRatio,
        onAspectRatioChange = { photoAspectRatio = it },
        photoPixelDepth = photoPixelDepth,
        onPixelDepthChange = { photoPixelDepth = it },
        selectedSensors = selectedSensors,
        modifier = Modifier
    )
}
