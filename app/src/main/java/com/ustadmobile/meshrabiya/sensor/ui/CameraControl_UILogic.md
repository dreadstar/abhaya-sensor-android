# Camera Controls (UI and Logic) extracted from SensorApp.kt

---

// Camera Controls: Camera/Flash toggles
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
    
    var flashModeState by remember { mutableStateOf(cameraController.getFlashMode()) }
    
    Button(
        onClick = { 
            cameraController.cycleFlash()
            flashModeState = cameraController.getFlashMode()
        },
        modifier = Modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = when (flashModeState) {
                CameraCapture.FlashMode.OFF -> Color.White.copy(alpha = 0.15f)
                CameraCapture.FlashMode.ON -> Color(0xFFFBBF24)
                CameraCapture.FlashMode.AUTO -> Color(0xFF3B82F6)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = "Cycle Flash Mode",
            tint = when (flashModeState) {
                CameraCapture.FlashMode.OFF -> Color.White
                CameraCapture.FlashMode.ON -> Color.Black
                CameraCapture.FlashMode.AUTO -> Color.White
            },
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = when (flashModeState) {
                CameraCapture.FlashMode.OFF -> "Flash: Off"
                CameraCapture.FlashMode.ON -> "Flash: On"
                CameraCapture.FlashMode.AUTO -> "Flash: Auto"
            },
            color = when (flashModeState) {
                CameraCapture.FlashMode.OFF -> Color.White
                CameraCapture.FlashMode.ON -> Color.Black
                CameraCapture.FlashMode.AUTO -> Color.White
            },
            fontSize = 12.sp
        )
    }
}

// Camera Controls: Frame Rate
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
            onClick = { videoFrameRate = fps },
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

// Camera Controls: Format
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
            onClick = { videoFormat = fmt },
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
