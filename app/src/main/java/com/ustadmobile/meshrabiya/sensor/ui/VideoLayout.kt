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
import com.google.accompanist.flowlayout.FlowRow
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.text.font.FontWeight
import com.ustadmobile.meshrabiya.sensor.camera.CameraController
// import com.ustadmobile.meshrabiya.sensor.capture.CameraCapture



@Composable
fun VideoLayout(
    cameraController: CameraController,
    videoFrameRate: Int,
    onFrameRateChange: (Int) -> Unit,
    videoFormat: String,
    onFormatChange: (String) -> Unit,
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
        // Start camera when this preview is shown
        LaunchedEffect(previewView) {
            cameraController.start(previewView, periodicSeconds = 5)
        }
        // Stop camera when this preview is removed
        DisposableEffect(Unit) {
            onDispose {
                if (!selectedSensors.contains("periodic_photos")) {
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
    // Camera Controls: Camera/Flash toggles and format/frame rate
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
       var torchEnabled by remember { mutableStateOf(cameraController.isTorchEnabled()) }
        Button(
            onClick = {
                cameraController.toggleTorch()
                torchEnabled = cameraController.isTorchEnabled()
            },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (torchEnabled) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Toggle Torch",
                tint = if (torchEnabled) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (torchEnabled) "Torch: On" else "Torch: Off",
                color = if (torchEnabled) Color.Black else Color.White,
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Frame Rate:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )
        listOf(24, 30, 60).forEach { rate ->
            Button(
                onClick = { onFrameRateChange(rate) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (videoFrameRate == rate)
                        Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = "$rate fps",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}
