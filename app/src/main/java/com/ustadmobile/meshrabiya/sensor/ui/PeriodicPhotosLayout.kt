package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.ustadmobile.meshrabiya.sensor.camera.CameraController
import com.google.accompanist.flowlayout.FlowRow
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.text.font.FontWeight
import com.ustadmobile.meshrabiya.sensor.capture.CameraCapture
import androidx.camera.core.ImageCapture

@Composable
fun PeriodicPhotosLayout(
    cameraController: CameraController,
    photoFrequency: Int,
    onPhotoFrequencyChange: (Int) -> Unit,
    photoAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    photoPixelDepth: String,
    onPixelDepthChange: (String) -> Unit,
    selectedSensors: Set<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.Black,
        elevation = 0.dp
    ) {
        val previewView = remember { PreviewView(context) }
        LaunchedEffect(previewView, photoFrequency) {
            cameraController.start(previewView, photoFrequency)
        }
        DisposableEffect(Unit) {
            onDispose {
                if (!selectedSensors.contains("camera_stream")) {
                    cameraController.stop()
                }
            }
        }
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    // Camera Controls Row 1: Camera/Flash toggles
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        Button(
            onClick = { cameraController.switchCamera() },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (cameraController.isFrontCamera()) "Front" else "Back",
                color = Color.White,
                fontSize = 12.sp
            )
        }
        

        // Lines 67–98

        var flashModeState by remember { mutableStateOf(cameraController.getFlashMode()) }
        Button(
            onClick = {
                cameraController.cycleFlash()
                flashModeState = cameraController.getFlashMode()
            },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (flashModeState) {
                    ImageCapture.FLASH_MODE_OFF -> Color.White.copy(alpha = 0.15f)
                    ImageCapture.FLASH_MODE_ON -> Color(0xFFFBBF24)
                    ImageCapture.FLASH_MODE_AUTO -> Color(0xFF3B82F6)
                    else -> Color.White.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Cycle Flash Mode",
                tint = when (flashModeState) {
                    ImageCapture.FLASH_MODE_OFF -> Color.White
                    ImageCapture.FLASH_MODE_ON -> Color.Black
                    ImageCapture.FLASH_MODE_AUTO -> Color.White
                    else -> Color.White
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (flashModeState) {
                    ImageCapture.FLASH_MODE_OFF -> "Flash: Off"
                    ImageCapture.FLASH_MODE_ON -> "Flash: On"
                    ImageCapture.FLASH_MODE_AUTO -> "Flash: Auto"
                    else -> "Flash: Off"
                },
                color = when (flashModeState) {
                    ImageCapture.FLASH_MODE_OFF -> Color.White
                    ImageCapture.FLASH_MODE_ON -> Color.Black
                    ImageCapture.FLASH_MODE_AUTO -> Color.White
                    else -> Color.White
                },
                fontSize = 12.sp
            )
        }
    }

 
    Spacer(modifier = Modifier.height(8.dp))
    // Photo Controls: Aspect Ratio & Pixel Depth
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 6.dp,
        crossAxisSpacing = 6.dp
    ) {
        Text(
            text = "Ratio:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
        listOf("4:3", "16:9", "1:1").forEach { ratio ->
            Button(
                onClick = { onAspectRatioChange(ratio) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (photoAspectRatio == ratio)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = ratio,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "Depth:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
        )
        listOf("8-bit", "10-bit").forEach { depth ->
            Button(
                onClick = { onPixelDepthChange(depth) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (photoPixelDepth == depth)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = depth,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    // Photo Controls: Capture Frequency
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 6.dp,
        crossAxisSpacing = 6.dp
    ) {
        Text(
            text = "Interval:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
        listOf(1, 3, 5, 10, 30, 60).forEach { sec ->
            Button(
                onClick = { onPhotoFrequencyChange(sec) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (photoFrequency == sec)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = "${sec}s",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}
