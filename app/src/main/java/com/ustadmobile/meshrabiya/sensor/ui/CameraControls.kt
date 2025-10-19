package com.ustadmobile.meshrabiya.sensor.ui

import android.content.Context
import androidx.compose.ui.viewinterop.AndroidView
import com.ustadmobile.meshrabiya.sensor.capture.CameraControllerImpl
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
// import removed: CameraCapture (not used)
import androidx.camera.view.LifecycleCameraController
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun VideoCameraControls(
    cameraControllerImpl: CameraControllerImpl,
    videoFrameRate: Int,
    onFrameRateChange: (Int) -> Unit,
    videoFormat: String,
    onFormatChange: (String) -> Unit,
    selectedSensors: Set<String>,
    onCameraStart: (PreviewView) -> Unit,
    onCameraStop: () -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    // Start camera when this preview is shown
    LaunchedEffect(previewView) {
        onCameraStart(previewView)
    }
    // Stop camera when this preview is removed
    DisposableEffect(Unit) {
        onDispose {
            onCameraStop()
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        Button(
            onClick = { cameraControllerImpl.switchCamera() },
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
                text = if (cameraControllerImpl.isFrontCamera()) "Front" else "Back",
                color = Color.White,
                fontSize = 12.sp
            )
        }
        var flashModeState by remember { mutableStateOf(cameraControllerImpl.getFlashMode()) }
        Button(
            onClick = {
                cameraControllerImpl.cycleFlash()
                flashModeState = cameraControllerImpl.getFlashMode()
            },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (flashModeState) {
                    0 -> Color.White.copy(alpha = 0.15f) // FLASH_MODE_OFF
                    1 -> Color(0xFFFBBF24) // FLASH_MODE_ON
                    2 -> Color(0xFF3B82F6) // FLASH_MODE_AUTO
                    else -> Color.White.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Cycle Flash Mode",
                tint = when (flashModeState) {
                    0 -> Color.White // FLASH_MODE_OFF
                    1 -> Color.Black // FLASH_MODE_ON
                    2 -> Color.White // FLASH_MODE_AUTO
                    else -> Color.White
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (flashModeState) {
                    0 -> "Flash: Off"
                    1 -> "Flash: On"
                    2 -> "Flash: Auto"
                    else -> "Flash: Off"
                },
                color = when (flashModeState) {
                    0 -> Color.White
                    1 -> Color.Black
                    2 -> Color.White
                    else -> Color.White
                },
                fontSize = 12.sp
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 6.dp,
        crossAxisSpacing = 6.dp
    ) {
        Text(
            text = "FPS:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
        listOf(15, 24, 30, 60).forEach { fps ->
            Button(
                onClick = { onFrameRateChange(fps) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (videoFrameRate == fps)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
 
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    text = "$fps",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 6.dp,
        crossAxisSpacing = 6.dp
    ) {
        Text(
            text = "Format:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
        listOf("H.264", "H.265", "VP9").forEach { fmt ->
            Button(
                onClick = { onFormatChange(fmt) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (videoFormat == fmt)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    text = fmt,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PhotoCameraControls(
    cameraControllerImpl: CameraControllerImpl,
    photoAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    photoPixelDepth: String,
    onPixelDepthChange: (String) -> Unit,
    photoFrequency: Int,
    onFrequencyChange: (Int) -> Unit,
    selectedSensors: Set<String>,
    onCameraStart: (PreviewView) -> Unit,
    onCameraStop: () -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    
    // Start camera when this preview is shown
    LaunchedEffect(previewView, photoFrequency) {
        onCameraStart(previewView)
    }
    // Stop camera when this preview is removed
    DisposableEffect(Unit) {
        onDispose {
            onCameraStop()
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { cameraControllerImpl.switchCamera() },
            modifier = Modifier.weight(1f).height(36.dp),
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
                text = if (cameraControllerImpl.isFrontCamera()) "Front" else "Back",
                color = Color.White,
                fontSize = 12.sp
            )
        }
        var flashModeState2 by remember { mutableStateOf(cameraControllerImpl.getFlashMode()) }
        Button(
            onClick = {
                cameraControllerImpl.cycleFlash()
                flashModeState2 = cameraControllerImpl.getFlashMode()
            },
            modifier = Modifier.weight(1f).height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (flashModeState2) {
                    0 -> Color.White.copy(alpha = 0.15f)
                    1 -> Color(0xFFFBBF24)
                    2 -> Color(0xFF3B82F6)
                    else -> Color.White.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Cycle Flash Mode",
                tint = when (flashModeState2) {
                    0 -> Color.White
                    1 -> Color.Black
                    2 -> Color.White
                    else -> Color.White
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (flashModeState2) {
                    0 -> "Flash: Off"
                    1 -> "Flash: On"
                    2 -> "Flash: Auto"
                    else -> "Flash: Off"
                },
                color = when (flashModeState2) {
                    0 -> Color.White
                    1 -> Color.Black
                    2 -> Color.White
                    else -> Color.White
                },
                fontSize = 12.sp
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
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
                onClick = { onFrequencyChange(sec) },
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
