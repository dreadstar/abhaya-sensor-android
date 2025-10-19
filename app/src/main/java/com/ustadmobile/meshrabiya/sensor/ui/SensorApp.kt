package com.ustadmobile.meshrabiya.sensor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.ustadmobile.meshrabiya.sensor.capture.AudioCapture
import com.ustadmobile.meshrabiya.sensor.capture.CameraCapture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.ustadmobile.meshrabiya.sensor.stream.HttpStreamIngestor
import java.nio.ByteBuffer
import java.nio.ByteOrder

// UI-facing ingestor type: exposes the same events flow as the app's in-process ingestor
private interface UIIngestor : com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor {
    data class UIEvent(val streamId: String, val timestampMs: Long, val payloadLength: Int)
    val events: SharedFlow<UIEvent>
}

@Composable
fun SensorApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Discover available hardware sensors
    val hardwareSensors = remember { mutableStateMapOf<String, Sensor>() }
    LaunchedEffect(Unit) {
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { s ->
            // Store by numeric type ID (e.g., "sensor_1") to match category sensor IDs
            val numericId = "sensor_${s.type}"
            hardwareSensors[numericId] = s
            // Also store by string type if available for reference
            s.stringType?.let { hardwareSensors[it] = s }
            // Debug logging
            android.util.Log.d("SensorApp", "Found sensor: $numericId (stringType=${s.stringType}, type=${s.type}, name=${s.name})")
        }
        android.util.Log.d("SensorApp", "Total sensors found: ${hardwareSensors.size}")
    }

    // State management
    val sensorCategories = remember { defaultSensorCategories() }
    var pollingFrequency by remember { mutableStateOf(10) }
    var selectedSensors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ingestorRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to start") }
    val ingestLog = remember { mutableStateListOf<String>() }
    val listeners = remember { mutableMapOf<String, SensorEventListener>() }
    
    // Camera & Audio control state
    var videoFrameRate by remember { mutableStateOf(30) }
    var videoFormat by remember { mutableStateOf("H.264") }
    var photoAspectRatio by remember { mutableStateOf("16:9") }
    var photoPixelDepth by remember { mutableStateOf("8-bit") }
    var photoFrequency by remember { mutableStateOf(5) } // seconds

    // Initialize HttpStreamIngestor with UI event tracking
    val ingestor = remember {
        val http = HttpStreamIngestor("https://example.com/store", token = null)
        val _events = MutableSharedFlow<UIIngestor.UIEvent>(replay = 0, extraBufferCapacity = 100)
        object : UIIngestor {
            override val events: SharedFlow<UIIngestor.UIEvent> = _events
            override fun start() = http.start()
            override fun stop() = http.stop()
            override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
                http.ingestSensorReading(streamId, timestampMs, payload)
                try {
                    _events.tryEmit(UIIngestor.UIEvent(streamId, timestampMs, payload.size))
                } catch (_: Throwable) {}
            }
        }
    }

    // Job for events collection so we can cancel it on stop
    var eventsJob by remember { mutableStateOf<Job?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            listeners.values.forEach { sensorManager.unregisterListener(it) }
            listeners.clear()
            eventsJob?.cancel()
            if (ingestorRunning) {
                ingestor.stop()
            }
        }
    }

    // When selection changes: register/unregister hardware sensor listeners
    LaunchedEffect(selectedSensors, hardwareSensors.keys.toList()) {
        val hwIds = hardwareSensors.keys
        val toRegister = selectedSensors.filter { it in hwIds } - listeners.keys
        val toUnregister = listeners.keys - selectedSensors

        toUnregister.forEach { id ->
            listeners[id]?.let { sensorManager.unregisterListener(it) }
            listeners.remove(id)
        }

        toRegister.forEach { id ->
            val sensor = hardwareSensors[id] ?: return@forEach
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    try {
                        val payload = floatsToByteArray(event.values)
                        val ts = System.currentTimeMillis()
                        ingestor.ingestSensorReading(id, ts, payload)
                    } catch (_: Exception) {}
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            listeners[id] = listener
        }
    }

    // Camera and audio capture controllers
    val cameraController = remember { CameraCapture(context, lifecycleOwner, ingestor) }
    val audioController = remember { AudioCapture(ingestor) }
    val audioLevel by audioController.audioLevel.collectAsState()

    // Start/stop audio based on selection (camera is managed by AndroidView)
    LaunchedEffect(selectedSensors) {
        if (selectedSensors.contains("audio_stream")) {
            audioController.start()
        } else {
            audioController.stop()
        }
    }

    // Start/stop functions
    val startIngestor = {
        if (!ingestorRunning) {
            ingestor.start()
            ingestorRunning = true
            statusMessage = "Streaming active"
            eventsJob = scope.launch {
                try {
                    (ingestor as? UIIngestor)?.events?.collectLatest { ev ->
                        ingestLog.add(0, "${ev.streamId} @ ${ev.timestampMs} (${ev.payloadLength} bytes)")
                        if (ingestLog.size > 200) ingestLog.removeLast()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    val stopIngestor = {
        if (ingestorRunning) {
            eventsJob?.cancel()
            eventsJob = null
            ingestor.stop()
            ingestorRunning = false
            statusMessage = "Stopped"
        }
    }

    // Gradient background colors (slate-900 to purple-900 to slate-900)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E293B), // slate-900
            Color(0xFF581C87), // purple-900
            Color(0xFF1E293B)  // slate-900
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // App Header
            AppHeader(
                isRunning = ingestorRunning,
                statusMessage = statusMessage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Polling Frequency Section
            PollingFrequencySection(
                currentFrequency = pollingFrequency,
                onFrequencyChange = { pollingFrequency = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sensors Selection with Categories
            GlassmorphicCard(title = "Select Sensors", titleTestTag = "select_sensors_card") {
                Column {
                    sensorCategories.forEach { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    expandedCategories = if (expandedCategories.contains(cat.name)) {
                                        expandedCategories - cat.name
                                    } else {
                                        expandedCategories + cat.name
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            elevation = 0.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    val isExpanded = expandedCategories.contains(cat.name)
                                    Text(
                                        text = if (isExpanded) "▼" else "▶",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                // Show sensors only if category is expanded
                                if (expandedCategories.contains(cat.name)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    cat.sensors.forEach { s ->
                                        val id = s.id
                                        val available = when (id) {
                                            "camera_stream", "audio_stream", "periodic_photos" -> true
                                            else -> hardwareSensors.containsKey(id)
                                        }

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .then(
                                                        if (!available) Modifier.background(
                                                            Color.Black.copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        ) else Modifier
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val checked = selectedSensors.contains(id)
                                                Checkbox(
                                                    checked = checked,
                                                    enabled = available,
                                                    onCheckedChange = { ch ->
                                                        selectedSensors = if (ch) {
                                                            selectedSensors + id
                                                        } else {
                                                            selectedSensors - id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF8B5CF6),
                                                        uncheckedColor = Color.White.copy(alpha = 0.4f),
                                                        disabledColor = Color.Gray.copy(alpha = 0.3f)
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = s.name,
                                                        fontSize = 14.sp,
                                                        color = if (available) Color.White else Color.Gray.copy(alpha = 0.5f)
                                                    )
                                                    if (id == "sensor_${Sensor.TYPE_AMBIENT_TEMPERATURE}") {
                                                        // Provide a stable test tag for the temperature display so tests can assert deterministically
                                                        Text(
                                                            text = "Temperature: --",
                                                            fontSize = 12.sp,
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            modifier = Modifier.semantics { testTag = "sensor_temperature" }
                                                        )
                                                    }
                                                    Text(
                                                        text = if (available) s.desc else "Not available on this device",
                                                        fontSize = 12.sp,
                                                        color = if (available) 
                                                            Color.White.copy(alpha = 0.6f)
                                                        else 
                                                            Color.Gray.copy(alpha = 0.4f),
                                                        fontStyle = if (!available) androidx.compose.ui.text.font.FontStyle.Italic else null
                                                    )
                                                }
                                                Text(
                                                    text = if (!available) "N/A" else if (checked) "ON" else "OFF",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        !available -> Color(0xFF9CA3AF)
                                                        checked -> Color(0xFF10B981)
                                                        else -> Color(0xFF6B7280)
                                                    }
                                                )
                                            }
                                            
                                            // Show camera preview when camera_stream (Video) is selected
                                            if (id == "camera_stream" && selectedSensors.contains(id)) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Column(modifier = Modifier.padding(start = 40.dp)) {
                                                    // Camera Preview
                                                    Card(
                                                        modifier = Modifier
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
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
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
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
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
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            
                                            // Show camera preview when periodic_photos is selected
                                            if (id == "periodic_photos" && selectedSensors.contains(id)) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Column(modifier = Modifier.padding(start = 40.dp)) {
                                                    // Camera Preview
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        backgroundColor = Color.Black,
                                                        elevation = 0.dp
                                                    ) {
                                                        val previewView = remember { PreviewView(context) }
                                                        
                                                        // Start camera when this preview is shown
                                                        LaunchedEffect(previewView, photoFrequency) {
                                                            cameraController.start(previewView, photoFrequency)
                                                        }
                                                        
                                                        // Stop camera when this preview is removed
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
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = { cameraController.switchCamera() },
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
                                                                text = if (cameraController.isFrontCamera()) "Front" else "Back",
                                                                color = Color.White,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                        
                                                        var flashModeState2 by remember { mutableStateOf(cameraController.getFlashMode()) }
                                                        
                                                        Button(
                                                            onClick = { 
                                                                cameraController.cycleFlash()
                                                                flashModeState2 = cameraController.getFlashMode()
                                                            },
                                                            modifier = Modifier.weight(1f).height(36.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                backgroundColor = when (flashModeState2) {
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
                                                                tint = when (flashModeState2) {
                                                                    CameraCapture.FlashMode.OFF -> Color.White
                                                                    CameraCapture.FlashMode.ON -> Color.Black
                                                                    CameraCapture.FlashMode.AUTO -> Color.White
                                                                },
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = when (flashModeState2) {
                                                                    CameraCapture.FlashMode.OFF -> "Flash: Off"
                                                                    CameraCapture.FlashMode.ON -> "Flash: On"
                                                                    CameraCapture.FlashMode.AUTO -> "Flash: Auto"
                                                                },
                                                                color = when (flashModeState2) {
                                                                    CameraCapture.FlashMode.OFF -> Color.White
                                                                    CameraCapture.FlashMode.ON -> Color.Black
                                                                    CameraCapture.FlashMode.AUTO -> Color.White
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
                                                                onClick = { photoAspectRatio = ratio },
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
                                                                onClick = { photoPixelDepth = depth },
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
                                                                onClick = { photoFrequency = sec },
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
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            
                                            // Show audio visualization when audio_stream is selected
                                            if (id == "audio_stream" && selectedSensors.contains(id)) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Column(modifier = Modifier.padding(start = 40.dp)) {
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        backgroundColor = Color.Black.copy(alpha = 0.3f),
                                                        elevation = 0.dp
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Phone,
                                                                    contentDescription = "Microphone",
                                                                    tint = Color(0xFF10B981),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Text(
                                                                    text = "Audio Recording Active",
                                                                    color = Color(0xFF10B981),
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            
                                                            // Audio Level Meter
                                                            Column(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Text(
                                                                    text = "Level: ${audioLevel.toInt()}%",
                                                                    color = Color.White.copy(alpha = 0.9f),
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                                
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                
                                                                // Horizontal level bar
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(24.dp)
                                                                        .clip(RoundedCornerShape(12.dp))
                                                                        .background(Color.White.copy(alpha = 0.1f))
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxHeight()
                                                                            .fillMaxWidth(audioLevel / 100f)
                                                                            .clip(RoundedCornerShape(12.dp))
                                                                            .background(
                                                                                when {
                                                                                    audioLevel > 75 -> Color(0xFFEF4444) // red
                                                                                    audioLevel > 50 -> Color(0xFFFBBF24) // yellow
                                                                                    else -> Color(0xFF10B981) // green
                                                                                }
                                                                            )
                                                                    )
                                                                }
                                                                
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                
                                                                // Visual bars
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                                ) {
                                                                    repeat(20) { index ->
                                                                        val barHeight = if (audioLevel > (index * 5)) {
                                                                            (8 + (index * 2)).dp
                                                                        } else {
                                                                            4.dp
                                                                        }
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .width(8.dp)
                                                                                .height(barHeight)
                                                                                .clip(RoundedCornerShape(2.dp))
                                                                                .background(
                                                                                    if (audioLevel > (index * 5)) {
                                                                                        when {
                                                                                            index > 15 -> Color(0xFFEF4444)
                                                                                            index > 10 -> Color(0xFFFBBF24)
                                                                                            else -> Color(0xFF10B981)
                                                                                        }
                                                                                    } else {
                                                                                        Color.White.copy(alpha = 0.2f)
                                                                                    }
                                                                                )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Event Log
            if (ingestLog.isNotEmpty()) {
                GlassmorphicCard(title = "Event Log", titleTestTag = "event_log") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ingestLog.forEach { logEntry ->
                            Text(
                                text = logEntry,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Control Buttons
            ControlButtons(
                isRunning = ingestorRunning,
                onStartClick = startIngestor,
                onStopClick = stopIngestor
            )
        }
    }
}

@Composable
private fun AppHeader(
    isRunning: Boolean,
    statusMessage: String
) {
    // Animated pulse for running indicator
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Abhaya Sensor",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning)
                                Color(0xFF10B981).copy(alpha = pulseAlpha)
                            else
                                Color(0xFF6B7280)
                        )
                )
                Text(
                    text = if (isRunning) "ACTIVE" else "STOPPED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) Color(0xFF10B981) else Color(0xFF9CA3AF),
                    modifier = Modifier.semantics { testTag = "app_status" }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = statusMessage,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }

}

@Composable
private fun GlassmorphicCard(
    title: String,
    titleTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White.copy(alpha = 0.1f),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .then(if (titleTestTag != null) Modifier.semantics { testTag = titleTestTag } else Modifier)
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PollingFrequencySection(
    currentFrequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    GlassmorphicCard(title = "Polling Frequency", titleTestTag = "polling_frequency") {
        Column {
            Text(
                text = "Current: $currentFrequency Hz",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency options in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10).forEach { freq ->
                    FrequencyButton(
                        frequency = freq,
                        isSelected = currentFrequency == freq,
                        onSelect = { onFrequencyChange(freq) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25, 50, 100).forEach { freq ->
                    FrequencyButton(
                        frequency = freq,
                        isSelected = currentFrequency == freq,
                        onSelect = { onFrequencyChange(freq) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyButton(
    frequency: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSelect,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected)
                Color(0xFF8B5CF6)
            else
                Color.White.copy(alpha = 0.1f),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "${frequency}Hz",
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = if (isRunning) onStopClick else onStartClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .semantics { testTag = "control_start_stop" },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isRunning)
                    Color(0xFFEF4444) // red
                else
                    Color(0xFF10B981), // green
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (isRunning) "Stop Streaming" else "Start Streaming",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper data classes
private data class SensorCategory(val name: String, val sensors: List<SensorDescriptor>)
private data class SensorDescriptor(val id: String, val name: String, val desc: String)

// Helper function to convert float array to byte array
private fun floatsToByteArray(values: FloatArray): ByteArray {
    val bb = ByteBuffer.allocate(4 * values.size).order(ByteOrder.nativeOrder())
    for (v in values) bb.putFloat(v)
    return bb.array()
}

// Define sensor categories
private fun defaultSensorCategories(): List<SensorCategory> {
    return listOf(
        SensorCategory("Motion Sensors", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_ACCELEROMETER}", "Accelerometer", "Device acceleration (m/s²)"),
            SensorDescriptor("sensor_${Sensor.TYPE_GYROSCOPE}", "Gyroscope", "Rotation rate (rad/s)"),
            SensorDescriptor("sensor_${Sensor.TYPE_MAGNETIC_FIELD}", "Magnetometer", "Magnetic field (μT)"),
            SensorDescriptor("sensor_${Sensor.TYPE_LINEAR_ACCELERATION}", "Linear Acceleration", "Acceleration without gravity"),
        )),
        SensorCategory("Environmental", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_AMBIENT_TEMPERATURE}", "Ambient Temperature", "Room temperature (°C)"),
            SensorDescriptor("sensor_${Sensor.TYPE_PRESSURE}", "Pressure", "Atmospheric pressure (hPa)"),
            SensorDescriptor("sensor_${Sensor.TYPE_RELATIVE_HUMIDITY}", "Humidity", "Relative humidity (%)"),
            SensorDescriptor("sensor_${Sensor.TYPE_LIGHT}", "Light", "Ambient light (lx)"),
        )),
        SensorCategory("Camera & Audio", listOf(
            SensorDescriptor("camera_stream", "Video", "Live video streaming with controls"),
            SensorDescriptor("audio_stream", "Audio Stream", "Audio capture with level monitoring"),
            SensorDescriptor("periodic_photos", "Periodic Photos", "Timed photo capture"),
        )),
        SensorCategory("System Metrics", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_HEART_BEAT}", "Heart Beat", "Heartbeat detection"),
            SensorDescriptor("sensor_${Sensor.TYPE_STEP_COUNTER}", "Step Counter", "Steps since reboot"),
            SensorDescriptor("sensor_${Sensor.TYPE_PROXIMITY}", "Proximity", "Distance to object (cm)"),
        ))
    )
}

